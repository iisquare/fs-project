package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.lm.core.RedisKey;
import com.iisquare.fs.web.lm.entity.Usage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网页搜索与抓取：按模型鉴权与计费，使用模型所属供应商转发。
 * model 仅用于网关内部校验，转发前会从请求体中移除。
 */
@Service
public class ToolsService {

    @Autowired
    CreditService creditService;
    @Autowired
    GatewayService gatewayService;
    @Autowired
    UsageService usageService;
    @Autowired
    RemindService remindService;
    @Autowired
    AIService aiService;
    @Autowired
    StringRedisTemplate redis;

    /** 网页搜索/抓取接口的单次读取超时：目标站点响应可能较慢 */
    private static final int TOOL_SOCKET_TIMEOUT = 60000;

    public static final Map<String, String> checkRates = new LinkedHashMap<>() {{
        put("request", "请求");
        put("token", "词元");
        put("credit", "积分");
    }};

    public String search(ObjectNode json, HttpServletRequest request, HttpServletResponse response) {
        return forward(json, request, response, "search");
    }

    public String fetch(ObjectNode json, HttpServletRequest request, HttpServletResponse response) {
        return forward(json, request, response, "fetch");
    }

    private String forward(ObjectNode json, HttpServletRequest request, HttpServletResponse response, String toolType) {
        long beginTime = System.currentTimeMillis();
        // 1. 密钥认证与积分检查
        ObjectNode auth = creditService.authByKey(token(request), request, gatewayService.cache());
        if (null == auth) {
            return error(response, 401, "invalid_request_error", "Authentication Fails, Your api key is invalid");
        }
        if (auth.at("/credit/remained").asDouble() <= 0) {
            return error(response, 400, "token_limited", "积分不足");
        }
        // 2. 模型鉴权：model 仅用于网关校验和计费，不转发给供应商
        String place = json.at("/model").asText("");
        if (DPUtil.empty(place)) {
            return error(response, 400, "model_required", "model 不能为空");
        }
        if (!auth.at("/models").has(place)) {
            return error(response, 403, "model_denied", "暂未开通该模型权限");
        }
        JsonNode model = gatewayService.minimumConcurrencyPriority(auth, place, request);
        if (null == model || model.isEmpty()) {
            return error(response, 403, "model_invalid", "模型暂不可用");
        }
        if (!toolType.equals(model.at("/type").asText(""))) {
            return error(response, 400, "model_type_mismatch", "模型类型不匹配");
        }
        JsonNode provider = gatewayService.cache().at("/providers/" + model.at("/providerId").asInt());
        if (provider.isEmpty()
                || DPUtil.empty(provider.at("/endpoint").asText(""))
                || DPUtil.empty(provider.at("/token").asText(""))) {
            return error(response, 503, "provider_invalid", "网页工具服务暂不可用");
        }
        // 3. 速率限制检查
        int uid = auth.at("/uid").asInt();
        for (JsonNode rate : auth.at("/rates")) {
            int rateId = rate.at("/id").asInt();
            for (Map.Entry<String, String> entry : checkRates.entrySet()) {
                double count = rate.at("/" + entry.getKey() + "Count").asDouble();
                int interval = rate.at("/" + entry.getKey() + "Interval").asInt();
                if (count <= 0 || interval <= 0) continue;
                String key = RedisKey.rate(entry.getKey(), uid, rateId, beginTime / interval / 1000);
                double current = DPUtil.parseDouble(redis.opsForValue().get(key));
                if (current >= count) {
                    remindService.limited(auth, entry);
                    return error(response, 400, "token_limited", "间隔内" + entry.getValue() + "数量超过限制");
                }
            }
        }
        // 4. 请求频次限制：按供应商与工具类型独立计数，每秒一个桶
        int qps = model.at("/content/qps").asInt();
        if (qps > 0) {
            String qpsKey = RedisKey.toolsQps(provider.at("/id").asInt(), toolType, beginTime / 1000);
            Long current = redis.opsForValue().increment(qpsKey);
            if (null == current) {
                return error(response, 503, "server_busy", "服务器繁忙，请稍后再试。");
            }
            if (1 == current) redis.expire(qpsKey, 2, TimeUnit.SECONDS);
            if (current > qps) {
                return error(response, 429, "rate_limited", "请求频次超过限制");
            }
        }
        // 5. 并发控制
        String keyParallel = RedisKey.modelParallel(model.at("/id").asInt());
        Long count = redis.opsForValue().increment(keyParallel);
        if (null == count) {
            return error(response, 400, "server_busy", "服务器繁忙，请稍后再试。");
        }
        redis.expire(keyParallel, 30, TimeUnit.MINUTES);
        // 6. 准备使用记录
        Usage.UsageBuilder usage = Usage.builder().type("consume_" + toolType).place(place);
        usage.uid(uid).authId(auth.at("/id").asInt())
                .modelId(model.at("/id").asInt()).providerId(provider.at("/id").asInt());
        usage.status("completed").requestIp(ServletUtil.getRemoteAddr(request)).beginTime(beginTime);
        usage.requestBody(DPUtil.stringify(json)).requestStream(0).requestHeader("");
        usage.responseHeader("").responseBody("").finishDetail("").auditDetail("");
        // 7. 移除 model 后转发，保持供应商接口的原始请求结构
        ObjectNode payload = json.deepCopy();
        payload.remove("model");
        String endpoint = provider.at("/endpoint").asText();
        String providerToken = provider.at("/token").asText();
        Map<String, Object> backendResult = aiService.post(endpoint + "/tools/" + toolType, payload,
                aiService.authorization(providerToken), TOOL_SOCKET_TIMEOUT);
        long endTime = System.currentTimeMillis();
        int status;
        String body;
        String finishReason;
        BigDecimal creditAmount = BigDecimal.ZERO;
        if (ApiUtil.failed(backendResult)) {
            int backendCode = ApiUtil.code(backendResult);
            status = backendCode > 15000 && backendCode < 15600 ? backendCode - 15000 : 502;
            Object data = ApiUtil.data(backendResult, Object.class);
            if (data instanceof JsonNode) {
                body = DPUtil.stringify(data);
            } else if (null != data) {
                body = data.toString();
            } else {
                body = "";
            }
            if (502 == status) { // 未拿到供应商报文时，给出工具服务自身的错误结构
                body = DPUtil.stringify(SsePlainEmitter.error("backend_failed",
                        ApiUtil.message(backendResult), "tools", null));
            }
            finishReason = "backend_error";
        } else {
            JsonNode data = ApiUtil.data(backendResult, JsonNode.class);
            body = null == data ? "{}" : DPUtil.stringify(data);
            status = 200;
            finishReason = "complete";
            // 按次计费：供应商成功响应即扣除单次费用
            creditAmount = new BigDecimal(model.at("/content/credits").asText("0")).abs();
        }
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        usage.creditAmount(creditAmount.negate()).responseBody(body).finishReason(finishReason);
        if (!"complete".equals(finishReason)) usage.finishDetail(body);
        usage.endTime(endTime).coastTotal((int) (endTime - beginTime));
        redis.opsForValue().decrement(keyParallel);
        usageService.record(usage.build(), auth);
        // 8. 成功后更新速率计数
        if (200 == status) {
            for (JsonNode rate : auth.at("/rates")) {
                int rateId = rate.at("/id").asInt();
                for (Map.Entry<String, String> entry : checkRates.entrySet()) {
                    int interval = rate.at("/" + entry.getKey() + "Interval").asInt();
                    if (interval <= 0) continue;
                    double delta = switch (entry.getKey()) {
                        case "request" -> 1;
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
        return body;
    }

    private String token(HttpServletRequest request) {
        String authorization = request.getHeader("authorization");
        if (null != authorization && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        }
        String xApiKey = request.getHeader("x-api-key");
        return null != xApiKey ? xApiKey : "";
    }

    private String error(HttpServletResponse response, int status, String code, String message) {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        return DPUtil.stringify(SsePlainEmitter.error(code, message, "invalid_request_error", null));
    }

}
