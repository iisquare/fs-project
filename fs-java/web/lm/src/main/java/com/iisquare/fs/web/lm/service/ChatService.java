package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.sse.SsePlainRequest;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.util.ChatUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
public class ChatService extends ServiceBase {

    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    AgentService agentService;
    @Autowired
    ProxyService proxyService;

    public SseEmitter demo(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SsePlainEmitter emitter = new SsePlainEmitter(0L);
        ObjectNode agent = agent(json, emitter, request, response);
        if (null == agent) return emitter.sync();
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
        SsePlainEmitter emitter = new SsePlainEmitter(0L);
        ObjectNode agent = agent(json, emitter, request, response);
        if (null == agent) return emitter.sync();
        ArrayNode messages = DPUtil.arrayNode();
        messages.addObject().put("role", "user").put("content", json.at("/input").asText(""));
        agent.replace("messages", messages);
        agent.replace("stream", json.at("/stream"));
        return completion(agent, emitter, request, response);
    }

    public SseEmitter dialog(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return null;
    }

    public ObjectNode agent(ObjectNode json, SsePlainEmitter emitter, HttpServletRequest request, HttpServletResponse response) {
        ObjectNode agents = agentService.listByIdentity(request, false);
        String agentId = json.at("/agentId").asText();
        if (!agents.has(agentId)) {
            ChatUtil.message(emitter, "error.agent_not_found", json);
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
            public boolean onMessage(CloseableHttpResponse response, String line, boolean isStream) {
                ObjectNode message = ChatUtil.parse(line);
                if (message.isEmpty()) return emitter.isRunning();
                if (message.has("error")) {
                    ChatUtil.message(emitter, "error.message", message);
                    return false;
                }
                if (message.has("choices")) {
                    ChatUtil.message(emitter, "choices.message", message.at("/choices"));
                    return emitter.isRunning();
                }
                ChatUtil.message(emitter, "error.unknown", message);
                return false;
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {
                ChatUtil.message(emitter, "error.throwable", throwable.getMessage()).abort();
            }
        };
        return proxyService.pool().process(req, emitter);
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
        String url = ServletUtil.getWebUrl(request, true);
        url += "/v1/chat/completions";
        HttpPost http = new HttpPost(url);
        http.addHeader("Authorization", "Bearer " + agent.at("/token").asText());
        http.addHeader("Content-Type", "application/json;charset=" + proxyService.charset.name());
        http.setEntity(new StringEntity(json.toString(), proxyService.charset));
        return http;
    }

}
