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
import com.iisquare.fs.web.lm.dao.*;
import com.iisquare.fs.web.lm.entity.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ProxyService extends ServiceBase implements MessageListener, InitializingBean, DisposableBean {

    @Autowired
    SensitiveService sensitiveService;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    ClientDao clientDao;
    @Autowired
    ClientEndpointDao clientEndpointDao;
    @Autowired
    ServerDao serverDao;
    @Autowired
    ServerEndpointDao serverEndpointDao;
    @Autowired
    LogDao logDao;

    private ObjectNode cache = DPUtil.objectNode();
    private final SsePlainRequestPool pool = new SsePlainRequestPool();
    public final Charset charset = StandardCharsets.UTF_8;

    @Override
    public void afterPropertiesSet() throws Exception {
        sensitiveService.rebuild();
        cache = cache();
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
                cache = cache();
                return;
        }
    }

    public SsePlainRequestPool pool() {
        return pool;
    }

    public Map<String, Object> notice(Map<String, Object> param) {
        String type = DPUtil.parseString(param.get("type"));
        if (!Arrays.asList("sensitive", "model").contains(type)) {
            return ApiUtil.result(1001, "类型异常", type);
        }
        ObjectNode notice = DPUtil.objectNode();
        notice.put("type", type);
        notice.put("time", System.currentTimeMillis());
        redis.convertAndSend(RedisKey.channel(), DPUtil.stringify(notice));
        return ApiUtil.result(0, null, notice);
    }

    /**
     * 配置缓存
     * {
     *     clients: {
     *         [token]: {
     *             id: Integer,
     *             name: String,
     *             token: String,
     *             endpoints: {
     *                 [serverId]: {
     *                     id: Integer,
     *                     clientId: Integer,
     *                     serverId: Integer,
     *                     parallel: Integer,
     *                     checkable: Boolean,
     *                 }
     *             }
     *         }
     *     },
     *     servers: {
     *         [model]: {
     *             id: Integer,
     *             model: String,
     *             name: String,
     *             endpoints: {
     *                 [id]: {
     *                     id: Integer,
     *                     serverId: Integer,
     *                     url: String,
     *                     model: String,
     *                     token: String,
     *                     parallel: Integer,
     *                 }
     *             }
     *         }
     *     }
     * }
     */
    public ObjectNode cache() {
        ObjectNode result = DPUtil.objectNode();
        List<Client> clients = clientDao.findAll((Specification<Client>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        ObjectNode jsonClients = result.putObject("clients");
        ObjectNode jsonClientsRefer = DPUtil.objectNode();
        for (Client client : clients) {
            ObjectNode item = jsonClients.putObject(client.getToken());
            item.put("id", client.getId());
            item.put("name", client.getName());
            item.put("token", client.getToken());
            item.putObject("endpoints");
            jsonClientsRefer.replace(String.valueOf(client.getId()), item);
        }
        List<ClientEndpoint> clientEndpoints = clientEndpointDao.findAll((Specification<ClientEndpoint>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        for (ClientEndpoint endpoint : clientEndpoints) {
            if (!jsonClientsRefer.has(String.valueOf(endpoint.getClientId()))) continue;
            ObjectNode endpoints = (ObjectNode) jsonClientsRefer.at("/" + endpoint.getClientId() + "/endpoints");
            ObjectNode item = endpoints.putObject(String.valueOf(endpoint.getServerId()));
            item.put("id", endpoint.getId());
            item.put("clientId", endpoint.getClientId());
            item.put("serverId", endpoint.getServerId());
            item.put("parallel", endpoint.getParallel());
            item.put("checkable", 1 == endpoint.getCheckable());
        }
        List<Server> servers = serverDao.findAll((Specification<Server>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        ObjectNode jsonServers = result.putObject("servers");
        ObjectNode jsonServersRefer = DPUtil.objectNode();
        for (Server server : servers) {
            ObjectNode item = jsonServers.putObject(server.getModel());
            item.put("id", server.getId());
            item.put("model", server.getModel());
            item.put("name", server.getName());
            item.putObject("endpoints");
            jsonServersRefer.replace(String.valueOf(server.getId()), item);
        }
        List<ServerEndpoint> serverEndpoints = serverEndpointDao.findAll((Specification<ServerEndpoint>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        for (ServerEndpoint endpoint : serverEndpoints) {
            if (!jsonServersRefer.has(String.valueOf(endpoint.getServerId()))) continue;
            ObjectNode endpoints = (ObjectNode) jsonServersRefer.at("/" + endpoint.getServerId() + "/endpoints");
            ObjectNode item = endpoints.putObject(String.valueOf(endpoint.getId()));
            item.put("id", endpoint.getId());
            item.put("serverId", endpoint.getServerId());
            item.put("url", endpoint.getUrl());
            item.put("model", endpoint.getModel());
            item.put("token", endpoint.getToken());
            item.put("parallel", endpoint.getParallel());
        }
        return result;
    }

    public String token(HttpServletRequest request) {
        String authorization = request.getHeader("authorization");
        if (null == authorization || !authorization.startsWith("Bearer ")) return "";
        return authorization.substring("Bearer ".length());
    }

    public ObjectNode models(String token) {
        ObjectNode result = DPUtil.objectNode();
        result.put("object", "list");
        ArrayNode data = result.putArray("data");
        Client client = clientDao.findOne((Specification<Client>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("token"), token));
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        }).orElse(null);
        if (null == client) return result;
        List<ClientEndpoint> endpoints = clientEndpointDao.findAll((Specification<ClientEndpoint>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("clientId"), client.getId()));
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        Set<Integer> serverIds = DPUtil.values(endpoints, Integer.class, "serverId");
        if (serverIds.size() == 0) return result;
        List<Server> servers = serverDao.findAll((Specification<Server>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("id").in(serverIds));
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("model")));
        for (Server server : servers) {
            ObjectNode item = data.addObject();
            item.put("id", server.getModel());
            item.put("object", "model");
            item.put("created", server.getCreatedTime() / 1000);
            item.put("owned_by", server.getName());
        }
        return result;
    }

    public SseEmitter completion(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Log.LogBuilder log = Log.builder();
        log.beginTime(System.currentTimeMillis());
        SsePlainEmitter emitter = new SsePlainEmitter(0L);
        JsonNode client = cache.at("/clients/" + token(request));
        if (client.isEmpty()) {
            return emitter.error("token_invalid", "无效的认证标识", "proxy", null, false).sync();
        }
        String model = json.at("/model").asText("");
        JsonNode server = cache.at("/servers/" + model);
        if (server.isEmpty()) {
            return emitter.error("model_invalid", "无效的模型名称", "proxy", null, false).sync();
        }
        JsonNode clientEndpoint = client.at("/endpoints/" + server.at("/id").asInt());
        if (clientEndpoint.isEmpty()) {
            return emitter.error("model_denied", "暂未开通该模型权限", "proxy", null, false).sync();
        }
        JsonNode serverEndpoints = server.at("/endpoints");
        if (serverEndpoints.isEmpty()) {
            return emitter.error("backend_invalid", "模型后端服务不可用", "proxy", null, false).sync();
        }
        int clientId = client.at("/id").asInt(0);
        int serverId = server.at("/id").asInt(0);
        int clientParallel = clientEndpoint.at("/parallel").asInt(0);
        String clientKey = RedisKey.clientParallel(clientId, serverId);
        if (clientParallel > 0) { // 客户端并发限制
            Long count = redis.opsForValue().increment(clientKey);
            if (null == count || count > clientParallel) {
                redis.opsForValue().decrement(clientKey);
                return emitter.error("client_busy", "客户端繁忙，请稍后再试。", "proxy", null, false).sync();
            } else {
                redis.expire(clientKey, 30, TimeUnit.MINUTES);
            }
        }
        JsonNode serverEndpoint = minimumConcurrencyPriority(serverEndpoints);
        if (null == serverEndpoint) {
            redis.opsForValue().decrement(clientKey);
            return emitter.error("server_busy", "服务端繁忙，请稍后再试。", "proxy", null, false).sync();
        }
        int backendId = serverEndpoint.at("/id").asInt(0);
        String backendKey = RedisKey.backendParallel(backendId);
        int backendParallel = serverEndpoint.at("/parallel").asInt(0);
        if (backendParallel > 0) { // 服务端并发限制
            Long count = redis.opsForValue().increment(backendKey);
            if (null == count || count > backendParallel) {
                redis.opsForValue().decrement(clientKey);
                redis.opsForValue().decrement(backendKey);
                return emitter.error("backend_busy", "模型端繁忙，请稍后再试。", "proxy", null, false).sync();
            } else {
                redis.expire(backendKey, 30, TimeUnit.MINUTES);
            }
        }
        // 基础校验和并发校验完成，进入受理阶段
        boolean stream = json.at("/stream").asBoolean(false);
        boolean checkable = clientEndpoint.at("/checkable").asBoolean(false);
        SsePlainRequest req = new SsePlainRequest() { // 处理请求
            @Override
            public HttpRequestBase request() {
                HttpPost request = new HttpPost(serverEndpoint.at("/url").asText(""));
                String token = serverEndpoint.at("/token").asText("");
                if (!DPUtil.empty(token)) {
                    request.addHeader("Authorization", "Bearer " + token);
                }
                request.addHeader("Content-Type", "application/json;charset=" + charset.name());
                json.put("model", serverEndpoint.at("/model").asText(""));
                json.put("stream", stream);
                request.setEntity(new StringEntity(json.toString(), charset));
                return request;
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) { // 中断客户端处理请求
                log.finishReason("backend_abort").finishDetail(ApiUtil.getStackTrace(throwable));
                emitter.error("backend_abort", "模型端服务处理异常", "proxy", throwable.getMessage(), isStream).abort();
            }

            @Override
            public void onClose() {
                log.responseTime(System.currentTimeMillis());
                log.responseBody(responseBody.toString()).responseCompletion(responseCompletion.toString());
                log.usagePromptTokens(promptTokens).usageCompletionTokens(completionTokens).usageTotalTokens(totalTokens);
                if (null != finishReason && null == log.build().getFinishReason()) {
                    log.finishReason(finishReason);
                }
                if (clientParallel > 0) {
                    redis.opsForValue().decrement(clientKey);
                }
                if (backendParallel > 0) {
                    redis.opsForValue().decrement(backendKey);
                }
                log.endTime(System.currentTimeMillis());
                logDao.save(log.build());
            }

            @Override
            public boolean onMessage(ObjectNode message, boolean isEvent, boolean isStream) {
                responseBody.append(DPUtil.stringify(message)).append("\n");
                ObjectNode data = data(message, isEvent);
                if (data.has("error")) {
                    log.finishReason("backend_error").finishDetail(DPUtil.stringify(data));
                    emitter.message(data, isEvent);
                    return false;
                } else if (data.has("choices")) {
                    List<String> check = check(data);
                    if (null != check && !check.isEmpty()) {
                        log.finishReason("completion_sensitive").finishDetail(DPUtil.implode(check));
                        emitter.error("completion_sensitive", "迷路了", "proxy", null, isStream);
                        return false;
                    }
                    data.put("model", model); // 替换模型名称
                }
                return emitter.message(message, isEvent).isRunning();
            }

            final StringBuilder responseBody = new StringBuilder();
            final StringBuilder responseCompletion = new StringBuilder();
            String finishReason = null;
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;
            boolean waitingSuffix = false;

            public List<String> check(ObjectNode message) {
                int length = responseCompletion.length();
                for (JsonNode choice : message.at("/choices")) {
                    boolean isDelta = choice.has("delta");
                    JsonNode item = isDelta ? choice.at("/delta") : choice.at("/message");
                    String reasoning = item.at("/reasoning_content").asText(null);
                    String content = item.at("/content").asText(null);
                    if (null != reasoning) {
                        if (0 == responseCompletion.length()) {
                            responseCompletion.append("<think>\n");
                            waitingSuffix = true; // 等待补充结束标识
                        }
                        responseCompletion.append(reasoning);
                    }
                    if (waitingSuffix && (!isDelta || null == reasoning)) {
                        responseCompletion.append("\n</think>\n");
                        waitingSuffix = false; // 完成补充结束标识
                    }
                    if (null != content) {
                        responseCompletion.append(content);
                    }
                    finishReason = choice.at("/finish_reason").asText();
                }
                if (message.has("usage")) {
                    promptTokens += message.at("/usage/prompt_tokens").asInt(0);
                    completionTokens += message.at("/usage/completion_tokens").asInt(0);
                    totalTokens += message.at("/usage/total_tokens").asInt(0);
                }
                if (!checkable) return null;
                String sentence = sensitiveService.window(responseCompletion.toString(), sensitiveService.window() + responseCompletion.length() - length);
                return sensitiveService.check(sentence);
            }
        };
        emitter.onError((e) -> {
            log.finishReason("client_abort").finishDetail(ApiUtil.getStackTrace(e));
            req.abort(); // 中断模型端处理请求
        }).onTimeout(() -> {
            log.finishReason("sse_timeout");
            req.abort(); // 中断模型端处理请求
        });
        // 记录请求信息
        log.clientId(clientId).clientEndpointId(clientEndpoint.at("/id").asInt(0));
        log.serverId(serverId).serverEndpointId(backendId);
        log.requestBody(DPUtil.stringify(json)).requestStream(stream ? 1 : 0);
        log.requestIp(ServletUtil.getRemoteAddr(request));
        log.requestPrompt("").responseBody("").responseCompletion("").finishDetail("").auditDetail(""); // 记录默认值
        String prompt = prompt(json);
        log.requestPrompt(prompt);
        if (checkable) { // 执行提示词拦截
            List<String> check = sensitiveService.check(prompt);
            if (check.size() > 0) {
                log.finishReason("prompt_sensitive").finishDetail(DPUtil.implode(check));
                return emitter.error("prompt_sensitive", "换个问题试试吧", "proxy", null, false).sync();
            }
        }
        // 发起模型端连接，处理客户端请求
        log.requestTime(System.currentTimeMillis());
        CloseableHttpResponse res;
        try {
            res = pool.execute(req);
            log.waitingTime(System.currentTimeMillis());
        } catch (Exception e) {
            log.finishReason("backend_failed").finishDetail(ApiUtil.getStackTrace(e));
            FileUtil.close(req);
            return emitter.error("connect_backend_failed", "连接模型端服务失败", "proxy", e.getMessage(), false).sync();
        }
        emitter.setMediaType(res); // 需要在异步返回前，确定请求响应类型
        return emitter.async(() -> pool.process(req, res));
    }

    public String prompt(JsonNode json) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode message : json.at("/messages")) {
            sb.append(message.at("/role").asText()).append(":\n");
            sb.append(message.at("/content").asText()).append("\n");
        }
        return sb.toString();
    }

    public JsonNode minimumConcurrencyPriority(JsonNode serverEndpoints) {
        List<String> backendKeys = new ArrayList<>();
        for (JsonNode endpoint : serverEndpoints) {
            backendKeys.add(RedisKey.backendParallel(endpoint.at("/id").asInt(0)));
        }
        List<String> backendValues = redis.opsForValue().multiGet(backendKeys);
        List<Integer> candidateIds = new ArrayList<>();
        Map<Integer, Double> sorts = new LinkedHashMap<>();
        int index = 0;
        for (JsonNode endpoint : serverEndpoints) {
            int id = endpoint.at("/id").asInt(0);
            double parallel = endpoint.at("/parallel").asInt(0);
            if (parallel > 0) {
                int count = DPUtil.parseInt(backendValues.get(index));
                if (count < parallel) {
                    sorts.put(id, DPUtil.parseDouble(count) / parallel);
                }
            } else {
                candidateIds.add(id);
            }
            index++;
        }
        if (sorts.size() > 0) {
            List<Map.Entry<Integer, Double>> list = new ArrayList<>(sorts.entrySet());
            list.sort((o1, o2) -> (int) ((o1.getValue() - o2.getValue()) * 10000));
            candidateIds.add(list.get(0).getKey());
        }
        if (candidateIds.size() == 0) return null;
        int random = DPUtil.random(0, candidateIds.size() - 1);
        return serverEndpoints.at("/" + candidateIds.get(random));
    }

}
