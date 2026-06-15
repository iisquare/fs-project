package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.sse.SsePlainRequest;
import com.iisquare.fs.base.web.sse.SsePlainRequestPool;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.lm.core.RedisKey;
import com.iisquare.fs.web.lm.entity.*;
import com.iisquare.fs.web.lm.gateway.GatewayHandler;
import com.iisquare.fs.web.lm.gateway.GatewayHandler.StreamContent;
import com.iisquare.fs.web.lm.gateway.GatewayHandler.StreamResult;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GatewayService extends ServiceBase implements MessageListener, InitializingBean, DisposableBean {

    @Autowired
    SensitiveService sensitiveService;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    ProviderService providerService;
    @Autowired
    CreditService creditService;
    @Autowired
    UsageService usageService;
    @Autowired
    RemindService remindService;

    private ObjectNode cache = DPUtil.objectNode();
    private final SsePlainRequestPool pool = new SsePlainRequestPool();
    public final Charset charset = StandardCharsets.UTF_8;

    public static final Map<String, String> checkRates = new LinkedHashMap<>(){{
        put("request", "请求");
        put("token", "词元");
        put("credit", "积分");
    }};

    @Override
    public void afterPropertiesSet() throws Exception {
        sensitiveService.rebuild();
        cache = providerService.cache();
    }

    @Override
    public void destroy() throws Exception {
        FileUtil.close(pool);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel(), StandardCharsets.UTF_8);
        if (!RedisKey.channel().equals(topic)) return;
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        JsonNode json = DPUtil.parseJSON(body);
        if (null == json) return;
        String type = json.at("/type").asText("");
        switch (type) {
            case "sensitive":
                sensitiveService.rebuild();
                return;
            case "model":
                cache = providerService.cache();
                return;
        }
    }

    public SsePlainRequestPool pool() {
        return pool;
    }

    public Map<String, Object> test(Map<String, Object> param, HttpServletRequest request) {
        String token = DPUtil.parseString(param.get("token"));
        ObjectNode auth = creditService.authByKey(token, cache);
        if (null == auth) {
            return ApiUtil.result(1001, "无效的认证密钥", token);
        }
        return remindService.amount(auth, 10);
    }

    public Map<String, Object> notice(Map<String, Object> param) {
        String type = DPUtil.parseString(param.get("type"));
        if (!Arrays.asList("sensitive", "model").contains(type)) {
            return ApiUtil.result(1001, "类型异常", type);
        }
        ObjectNode notice = DPUtil.objectNode();
        notice.put("type", type);
        notice.put("time", System.currentTimeMillis());
        redis.convertAndSend(RedisKey.channel(), notice.toString());
        return ApiUtil.result(0, null, notice);
    }

    public String token(HttpServletRequest request) {
        String authorization = request.getHeader("authorization");
        if (null != authorization && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        }
        String xApiKey = request.getHeader("x-api-key");
        return null != xApiKey ? xApiKey : "";
    }

    public static ObjectNode error401(HttpServletResponse response) {
        response.setStatus(401);
        return SsePlainEmitter.error("invalid_request_error", "Authentication Fails, Your api key is invalid", "authentication_error", null);
    }

    public ObjectNode models(Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) {
        ObjectNode auth = creditService.authByKey(token(request), cache);
        if (null == auth) return error401(response);
        boolean isAnthropic = isAnthropicModelsRequest(request);
        ObjectNode result = DPUtil.objectNode();
        if (isAnthropic) {
            result.put("type", "model_list");
        } else {
            result.put("object", "list");
        }
        ArrayNode data = result.putArray("data");
        for (Map.Entry<String, JsonNode> entry : auth.at("/models").properties()) {
            ObjectNode item = data.addObject();
            item.put("id", entry.getKey());
            if (isAnthropic) {
                item.put("type", "model");
                item.put("display_name", entry.getKey());
                item.put("created_at", "2025-01-01T00:00:00Z");
            } else {
                item.put("object", "model");
                item.put("owned_by", "fs-gateway");
            }
        }
        return result;
    }

    private boolean isAnthropicModelsRequest(HttpServletRequest request) {
        String version = request.getHeader("anthropic-version");
        if (!DPUtil.empty(version)) return true;
        String authorization = request.getHeader("authorization");
        if (null != authorization && authorization.startsWith("Bearer ")) return false;
        String xApiKey = request.getHeader("x-api-key");
        return !DPUtil.empty(xApiKey);
    }

    public SseEmitter completion(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        long beginTime = System.currentTimeMillis();
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        ObjectNode auth = creditService.authByKey(token(request), cache);
        if (null == auth) {
            return emitter.error("token_invalid", "无效的认证标识", "gateway", null, false).sync(401);
        }
        if (auth.at("/credit/remained").asDouble() <= 0) {
            return emitter.error("token_limited", "积分不足", "gateway", null, false).sync(400);
        }
        String place = json.at("/model").asText("");
        if (DPUtil.empty(place) || !auth.at("/models").has(place)) {
            return emitter.error("model_denied", "暂未开通该模型权限", "gateway", null, false).sync(403);
        }
        JsonNode model = minimumConcurrencyPriority(place);
        if (null == model || model.isEmpty()) {
            return emitter.error("model_invalid", "模型暂不可用", "gateway", null, false).sync(403);
        }
        JsonNode provider = cache.at("/providers/" + model.at("/providerId").asInt());
        if (provider.isEmpty()) {
            return emitter.error("provider_invalid", "提供商暂不可用", "gateway", null, false).sync(403);
        }
        int uid = auth.at("/uid").asInt();
        for (JsonNode rate : auth.at("/credit/rates")) { // 用户访问频率限制
            int rateId = rate.at("/id").asInt();
            for (Map.Entry<String, String> entry : checkRates.entrySet()) {
                double count = rate.at("/" + entry.getKey() + "Count").asDouble();
                int interval = rate.at("/" + entry.getKey() + "Interval").asInt();
                if (count <= 0 || interval <= 0) continue;
                String key = RedisKey.rate(entry.getKey(), uid, rateId, beginTime / interval / 1000);
                double current = DPUtil.parseDouble(redis.opsForValue().get(key));
                if (current >= count) {
                    remindService.limited(auth, entry);
                    return emitter.error("token_limited", "间隔内" + entry.getValue() + "数量超过限制", "gateway", null, false).sync(400);
                }
            }
        }
        String keyParallel = RedisKey.modelParallel(model.at("/id").asInt());
        Long count = redis.opsForValue().increment(keyParallel);
        if (null == count) {
            return emitter.error("server_busy", "服务器繁忙，请稍后再试。", "gateway", null, false).sync(400);
        } else {
            redis.expire(keyParallel, 30, TimeUnit.MINUTES);
        }
        Usage.UsageBuilder usage = Usage.builder().type("consume_llm").place(place);
        usage.uid(auth.at("/uid").asInt()).authId(auth.at("/id").asInt());
        usage.modelId(model.at("/id").asInt()).providerId(provider.at("/id").asInt());
        usage.status("completed").requestIp(ServletUtil.getRemoteAddr(request)).beginTime(beginTime);
        // 基础校验和并发校验完成，进入受理阶段
        GatewayHandler handler = GatewayHandler.select(request, provider);
        if (null == handler) {
            return emitter.error("protocol_mismatch", "客户端与后端协议不兼容，当前仅支持同协议转发", "gateway", null, false).sync(403);
        }
        boolean stream = json.at("/stream").asBoolean(false);
        boolean securityDetectable = model.at("/securityDetectable").asBoolean(false);
        SsePlainRequest req = new SsePlainRequest() { // 处理请求
            @Override
            public HttpRequestBase request() {
                try {
                    return handler.buildRequest(json, model, provider);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) { // 中断客户端处理请求
                usage.finishReason("backend_abort").finishDetail(ApiUtil.getStackTrace(throwable));
                emitter.error("backend_abort", "模型端服务处理异常", "gateway", throwable.getMessage(), isStream).abort();
            }

            @Override
            public void onClose() {
                buildMergedToolCalls();
                usage.responseBody(responseBody.toString()).responseTool(responseTool.toString())
                        .responseReason(responseReason.toString()).responseCompletion(responseCompletion.toString());
                if (totalTokens <= 0) totalTokens = promptTokens + completionTokens;
                usage.usagePromptCachedTokens(promptCachedTokens)
                    .usagePromptTokens(promptTokens).usageCompletionTokens(completionTokens).usageTotalTokens(totalTokens);
                if (null != finishReason && null == usage.getFinishReason()) {
                    usage.finishReason(finishReason);
                }
                redis.opsForValue().decrement(keyParallel);
                long endTime = System.currentTimeMillis();
                usage.endTime(endTime).coastTotal((int) (endTime - beginTime));
                BigDecimal creditAmount = BigDecimal.ZERO;
                BigDecimal divisor = BigDecimal.valueOf(1000000);
                creditAmount = creditAmount.add(BigDecimal.valueOf(promptCachedTokens).abs()
                        .multiply(new BigDecimal(model.at("/content/prompt_cached_credits").asText()).abs())
                        .divide(divisor, 20, RoundingMode.HALF_UP));
                creditAmount = creditAmount.add(BigDecimal.valueOf(promptTokens - promptCachedTokens).abs()
                        .multiply(new BigDecimal(model.at("/content/prompt_credits").asText()).abs())
                        .divide(divisor, 20, RoundingMode.HALF_UP));
                creditAmount = creditAmount.add(BigDecimal.valueOf(completionTokens).abs()
                        .multiply(new BigDecimal(model.at("/content/completion_credits").asText()).abs())
                        .divide(divisor, 20, RoundingMode.HALF_UP));
                usage.creditAmount(creditAmount.negate());
                usageService.record(usage.build(), auth);
                for (JsonNode rate : auth.at("/credit/rates")) { // 用户访问频率限制
                    int rateId = rate.at("/id").asInt();
                    for (Map.Entry<String, String> entry : checkRates.entrySet()) {
                        int interval = rate.at("/" + entry.getKey() + "Interval").asInt();
                        if (interval <= 0) continue;
                        double delta = switch (entry.getKey()) {
                            case "request" -> 1;
                            case "token" -> totalTokens;
                            case "credit" -> creditAmount.doubleValue();
                            default -> 0;
                        };
                        if (delta <= 0) continue;
                        String key = RedisKey.rate(entry.getKey(), uid, rateId, beginTime / interval / 1000);
                        redis.opsForValue().increment(key, delta);
                        redis.expire(key, interval, TimeUnit.SECONDS);
                    }
                }
            }

            @Override
            public boolean onMessage(ObjectNode message, boolean isEvent, boolean isStream) {
                responseBody.append(DPUtil.stringify(message)).append("\n");
                ObjectNode data;
                ObjectNode sseMessage;
                boolean sseIsEvent;
                if (!isStream) {
                    // Non-streaming: full response body from backend
                    ObjectNode backendData = GatewayHandler.parseSseData(message, isEvent);
                    data = handler.processNonStreamResponse(backendData);
                    sseIsEvent = true;
                    sseMessage = DPUtil.objectNode();
                    sseMessage.put("data", DPUtil.stringify(data));
                } else {
                    // Streaming: SSE event from backend
                    StreamResult sr = handler.processStreamMessage(message, isEvent);
                    if (!sr.forward) return isRunning();
                    data = sr.data;
                    sseMessage = sr.sseMessage;
                    sseIsEvent = sr.isEvent;
                    if (data.has("_done")) return isRunning();
                }
                if (data.has("error")) {
                    usage.finishReason("backend_error").finishDetail(DPUtil.stringify(data));
                    emitter.message(sseMessage, sseIsEvent);
                    return false;
                }
                // Accumulate content/tokens via handler
                StreamContent sc = handler.extractStreamContent(data);
                if (sc.hasError) {
                    usage.finishReason("backend_error").finishDetail(DPUtil.stringify(data));
                    emitter.message(sseMessage, sseIsEvent);
                    return false;
                }
                if (sc.reasoning != null) responseReason.append(sc.reasoning);
                if (sc.content != null) responseCompletion.append(sc.content);
                if (sc.toolCallsJson != null) mergeToolCallChunk(sc.toolCallsJson);
                if (sc.finishReason != null) finishReason = sc.finishReason;
                promptCachedTokens += sc.cachedPromptTokens;
                promptTokens += sc.promptTokens;
                completionTokens += sc.completionTokens;
                totalTokens += sc.totalTokens;
                // Replace model name
                if (!data.isEmpty() && !data.has("_done")) {
                    data.put("model", place);
                    sseMessage.put("data", DPUtil.stringify(data));
                }
                // Sensitive word check
                if (securityDetectable && (sc.reasoning != null || sc.content != null)) {
                    List<String> check = checkAccumulated();
                    if (!check.isEmpty()) {
                        usage.finishReason("completion_sensitive").finishDetail(DPUtil.implode(check));
                        emitter.error("completion_sensitive", "迷路了", "gateway", null, isStream);
                        return false;
                    }
                }
                return emitter.message(sseMessage, sseIsEvent).isRunning();
            }

            private boolean isRunning() {
                return !isAborted();
            }

            final StringBuilder responseBody = new StringBuilder();
            final StringBuilder responseReason = new StringBuilder();
            final StringBuilder responseCompletion = new StringBuilder();
            final StringBuilder responseTool = new StringBuilder();
            final Map<Integer, ObjectNode> toolCallMeta = new LinkedHashMap<>();
            final Map<Integer, StringBuilder> toolCallArgBuilders = new LinkedHashMap<>();
            String finishReason = null;
            int promptCachedTokens = 0, promptTokens = 0, completionTokens = 0, totalTokens = 0;

            public List<String> checkAccumulated() {
                List<String> check = new ArrayList<>();
                int lenReason = responseReason.length();
                int lenCompletion = responseCompletion.length();
                if (lenReason > 0) {
                    check.addAll(sensitiveService.check(sensitiveService.window(responseReason.toString(), lenReason + sensitiveService.window())));
                }
                if (lenCompletion > 0) {
                    check.addAll(sensitiveService.check(sensitiveService.window(responseCompletion.toString(), lenCompletion + sensitiveService.window())));
                }
                return check;
            }

            private void mergeToolCallChunk(String json) {
                JsonNode parsed = DPUtil.parseJSON(json);
                if (parsed == null || !parsed.isArray()) {
                    responseTool.append(json).append("\n");
                    return;
                }
                for (JsonNode item : parsed) {
                    int index = item.at("/index").asInt();
                    if (item.has("id") && item.has("function") && item.at("/function").has("name")) {
                        toolCallMeta.putIfAbsent(index, item.deepCopy());
                    }
                    String args = item.at("/function/arguments").asText(null);
                    if (args != null) {
                        toolCallArgBuilders.computeIfAbsent(index, k -> new StringBuilder()).append(args);
                    }
                }
            }

            private void buildMergedToolCalls() {
                if (toolCallMeta.isEmpty()) return;
                ArrayNode merged = DPUtil.arrayNode();
                for (Map.Entry<Integer, ObjectNode> entry : toolCallMeta.entrySet()) {
                    ObjectNode item = entry.getValue();
                    StringBuilder argBuilder = toolCallArgBuilders.get(entry.getKey());
                    String mergedArgs = argBuilder != null ? argBuilder.toString() : "";
                    if (item.has("function")) {
                        ((ObjectNode) item.at("/function")).put("arguments", mergedArgs);
                    }
                    merged.add(item);
                }
                responseTool.append(merged.toPrettyString());
            }
        };
        emitter.onError((e) -> {
            usage.finishReason("client_abort").finishDetail(ApiUtil.getStackTrace(e));
            req.abort(); // 中断模型端处理请求
        }).onTimeout(() -> {
            usage.finishReason("sse_timeout");
            req.abort(); // 中断模型端处理请求
        });
        // 记录请求信息
        usage.requestBody(DPUtil.stringify(json)).requestStream(stream ? 1 : 0);
        usage.requestIp(ServletUtil.getRemoteAddr(request));
        usage.responseBody("").responseCompletion("").finishDetail("").auditDetail(""); // 记录默认值
        String prompt = prompt(json, usage);
        if (securityDetectable) { // 执行提示词拦截
            List<String> check = sensitiveService.check(prompt);
            if (!check.isEmpty()) {
                usage.finishReason("prompt_sensitive").finishDetail(DPUtil.implode(check));
                return emitter.error("prompt_sensitive", "换个问题试试吧", "gateway", null, false).sync(403);
            }
        }
        // 发起模型端连接，处理客户端请求
        CloseableHttpResponse res;
        try {
            res = pool.execute(req);
        } catch (Exception e) {
            usage.finishReason("backend_failed").finishDetail(ApiUtil.getStackTrace(e));
            FileUtil.close(req);
            return emitter.error("connect_backend_failed", "连接模型端服务失败", "gateway", e.getMessage(), false).sync(400);
        }
        emitter.setMediaType(res); // 需要在异步返回前，确定请求响应类型
        return emitter.async(() -> pool.process(req, res));
    }

    private String extractMessageContent(JsonNode message) {
        JsonNode content = message.at("/content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.at("/type").asText())) {
                    sb.append(block.at("/text").asText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * Extract tool call / tool result info from a message, covering both
     * OpenAI (tool_calls on assistant, tool_call_id on tool role) and
     * Anthropic (tool_use / tool_result blocks in content array) formats.
     */
    private String extractMessageTools(JsonNode message) {
        StringBuilder sb = new StringBuilder();
        // --- OpenAI format ---
        // assistant message with tool_calls
        JsonNode toolCalls = message.at("/tool_calls");
        if (toolCalls.isArray() && !toolCalls.isEmpty()) {
            sb.append("tool_calls:\n");
            for (JsonNode tc : toolCalls) {
                sb.append("  - id: ").append(tc.at("/id").asText()).append("\n");
                sb.append("    function: ").append(tc.at("/function/name").asText()).append("\n");
                sb.append("    arguments: ").append(tc.at("/function/arguments").asText()).append("\n");
            }
        }
        // tool message with tool_call_id (OpenAI)
        if ("tool".equals(message.at("/role").asText())) {
            sb.append("[tool_call_id: ").append(message.at("/tool_call_id").asText()).append("]\n");
        }
        // --- Anthropic format ---
        JsonNode content = message.at("/content");
        if (content.isArray()) {
            for (JsonNode block : content) {
                String type = block.at("/type").asText();
                if ("tool_use".equals(type)) {
                    sb.append("tool_use:\n");
                    sb.append("  - id: ").append(block.at("/id").asText()).append("\n");
                    sb.append("    name: ").append(block.at("/name").asText()).append("\n");
                    sb.append("    input: ").append(DPUtil.stringify(block.at("/input"))).append("\n");
                } else if ("tool_result".equals(type)) {
                    sb.append("tool_result:\n");
                    sb.append("  - tool_use_id: ").append(block.at("/tool_use_id").asText()).append("\n");
                    JsonNode tc = block.at("/content");
                    if (tc.isTextual()) {
                        sb.append("    content: ").append(tc.asText()).append("\n");
                    } else if (tc.isArray()) {
                        for (JsonNode b : tc) {
                            if ("text".equals(b.at("/type").asText())) {
                                sb.append("    content: ").append(b.at("/text").asText()).append("\n");
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public String prompt(JsonNode json, Usage.UsageBuilder usage) {
        StringBuilder sb = new StringBuilder();
        String systemContent = extractSystemContent(json);
        usage.requestSystem(systemContent);
        if (!systemContent.isEmpty()) {
            sb.append("[system]\n").append(systemContent).append("\n");
        }
        String lastUserContent = "";
        for (JsonNode message : json.at("/messages")) {
            String role = message.at("/role").asText();
            String content = extractMessageContent(message);
            String tools = extractMessageTools(message);
            sb.append("[").append(role).append("]\n");
            sb.append(content);
            if (!tools.isEmpty()) {
                sb.append("\n").append(tools);
            }
            sb.append("\n");
            if ("system".equals(role) && systemContent.isEmpty()) {
                systemContent = content;
                usage.requestSystem(systemContent);
            }
            if ("user".equals(role)) {
                lastUserContent = content;
            }
        }
        usage.requestUser(lastUserContent);
        String prompt = sb.toString();
        usage.requestPrompt(prompt);
        return prompt;
    }

    private String extractSystemContent(JsonNode json) {
        // Anthropic format: top-level "system" field (string or array of content blocks)
        if (json.has("system")) {
            JsonNode sys = json.at("/system");
            if (sys.isTextual()) return sys.asText();
            if (sys.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode block : sys) {
                    if ("text".equals(block.at("/type").asText())) {
                        sb.append(block.at("/text").asText());
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }

    public JsonNode minimumConcurrencyPriority(String model) {
        List<Integer> modelIds = DPUtil.toJSON(cache.at("/aliases/" + model.replaceAll("/", "~1")), List.class);
        if (DPUtil.empty(modelIds)) return null;
        int size = modelIds.size();
        if (1 == size) return cache.at("/models/" + modelIds.get(0));
        List<String> keys = new ArrayList<>();
        for (Integer modelId : modelIds) {
            keys.add(RedisKey.modelParallel(modelId));
        }
        List<String> values = redis.opsForValue().multiGet(keys);
        if (null == values) return null;
        List<Integer> candidateIds = new ArrayList<>();
        Map<Integer, Double> sorts = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            Integer id = modelIds.get(index);
            int rpm = cache.at("/models/" + id + "/content/rpm").asInt();
            if (rpm > 0) {
                int count = DPUtil.parseInt(values.get(index));
                if (count < rpm) {
                    sorts.put(id, DPUtil.parseDouble(count) / rpm);
                }
            } else {
                candidateIds.add(id);
            }
        }
        if (!sorts.isEmpty()) {
            List<Map.Entry<Integer, Double>> list = new ArrayList<>(sorts.entrySet());
            list.sort((o1, o2) -> (int) ((o1.getValue() - o2.getValue()) * 10000));
            candidateIds.add(list.get(0).getKey());
        }
        if (candidateIds.isEmpty()) return null;
        int random = DPUtil.random(0, candidateIds.size() - 1);
        return cache.at("/models/" + candidateIds.get(random));
    }

}
