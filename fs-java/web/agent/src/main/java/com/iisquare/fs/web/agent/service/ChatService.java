package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.sse.SsePlainRequest;
import com.iisquare.fs.base.web.sse.SsePlainRequestPool;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Service
public class ChatService extends ServiceBase implements DisposableBean {

    public final Charset charset = StandardCharsets.UTF_8;
    private final SsePlainRequestPool pool = new SsePlainRequestPool();
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    AgentService agentService;
    @Value("${rpc.lm.rest}")
    private String gatewayEndpoint; // 模型网关地址，对话经 /v1/chat/completions 转发

    @Override
    public void destroy() throws Exception {
        FileUtil.close(pool);
    }

    /**
     * 当前用户可见的智能体（应用）列表，供模型调试、模型对话、模型对比选择使用
     */
    public ObjectNode agents(HttpServletRequest request) {
        return agentService.listByIdentity(request, true);
    }

    public SseEmitter dialog(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        ObjectNode agent = agent(json, emitter, request, response);
        if (null == agent) return emitter.sync(404);
        ArrayNode messages = DPUtil.arrayNode();
        messages.addObject().put("role", "user").put("content", json.at("/input").asText(""));
        agent.replace("messages", messages);
        agent.put("stream", true);
        return completion(agent, emitter, request, response);
    }

    public SseEmitter demo(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        ObjectNode agent = agent(json, emitter, request, response);
        if (null == agent) return emitter.sync(404);
        agent.replace("systemPrompt", json.at("/systemPrompt"));
        agent.replace("maxTokens", json.at("/maxTokens"));
        agent.replace("temperature", json.at("/temperature"));
        agent.replace("parameter", json.at("/parameter"));
        ArrayNode messages = DPUtil.arrayNode();
        messages.addObject().put("role", "user").put("content", json.at("/input").asText(""));
        agent.replace("messages", messages);
        agent.replace("stream", json.at("/stream"));
        return completion(agent, emitter, request, response);
    }

    public SseEmitter compare(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        ObjectNode agent = agent(json, emitter, request, response);
        if (null == agent) return emitter.sync(404);
        ArrayNode messages = DPUtil.arrayNode();
        messages.addObject().put("role", "user").put("content", json.at("/input").asText(""));
        agent.replace("messages", messages);
        agent.replace("stream", json.at("/stream"));
        return completion(agent, emitter, request, response);
    }

    public ObjectNode agent(ObjectNode json, SsePlainEmitter emitter, HttpServletRequest request, HttpServletResponse response) {
        ObjectNode agents = agentService.listByIdentity(request, false);
        String agentId = json.at("/agentId").asText();
        if (!agents.has(agentId)) {
            message(emitter, "error.agent_not_found", json);
            return null;
        }
        return (ObjectNode) agents.get(agentId);
    }

    public SseEmitter completion(ObjectNode agent, SsePlainEmitter emitter, HttpServletRequest request, HttpServletResponse response) {
        HttpRequestBase http = request(request, agent,
                (ArrayNode) agent.at("/messages"),
                agent.at("/stream").asBoolean(false));
        SsePlainRequest req = new SsePlainRequest() {
            @Override
            public HttpRequestBase request() throws Exception {
                return http;
            }

            @Override
            public boolean onMessage(ObjectNode message, boolean isEvent, boolean isStream) {
                if (isComment(message, isEvent)) { // 直接转发心跳包和结束标识
                    return emitter.message(message, isEvent).isRunning();
                }
                ObjectNode data = data(message, isEvent);
                if (data.has("error")) {
                    message(emitter, "error.message", data);
                    return false;
                }
                if (data.has("choices")) {
                    message(emitter, "choices.message", data.at("/choices"));
                    return emitter.isRunning();
                }
                message(emitter, "error.unknown", data);
                return false;
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {
                message(emitter, "error.throwable", throwable.getMessage()).abort();
            }
        };
        return pool.process(req, emitter);
    }

    public HttpRequestBase request(HttpServletRequest request, JsonNode agent, ArrayNode messages, boolean stream) {
        ObjectNode json = DPUtil.objectNode();
        JsonNode parameter = DPUtil.parseJSON(agent.at("/parameter").asText());
        if (null != parameter && parameter.isObject()) {
            json.setAll((ObjectNode) parameter);
        }
        String systemPrompt = agent.at("/systemPrompt").asText();
        if (!DPUtil.empty(systemPrompt)) {
            ObjectNode system = messages.insertObject(0);
            system.put("role", "system");
            system.put("content", systemPrompt);
        }
        int maxTokens = agent.at("/maxTokens").asInt(0);
        double temperature = agent.at("/temperature").asDouble(0);
        json.put("model", agent.at("/model").asText(""));
        if (maxTokens > 0) json.put("max_tokens", maxTokens);
        if (temperature > 0) json.put("temperature", temperature);
        json.put("stream", stream);
        json.replace("messages", messages);
        String url = gatewayEndpoint + "/v1/chat/completions";
        HttpPost http = new HttpPost(url);
        http.addHeader("Authorization", "Bearer " + agent.at("/token").asText());
        http.addHeader("Content-Type", "application/json;charset=" + charset.name());
        http.setEntity(new StringEntity(json.toString(), charset));
        return http;
    }

    public String message(String action, Object data) {
        ObjectNode message = DPUtil.objectNode();
        message.put("action", action);
        message.replace("data", DPUtil.toJSON(data));
        return message.toString();
    }

    public SsePlainEmitter message(SsePlainEmitter emitter, String action, Object data) {
        return emitter.data(message(action, data));
    }

}
