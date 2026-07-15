package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Abstract handler for LLM gateway request forwarding.
 *
 * Two concrete implementations handle pass-through forwarding:
 *   OpenAI client → OpenAI-compatible backend, Anthropic client → Anthropic backend.
 *
 * Cross-format mismatches (e.g. Anthropic client → OpenAI backend) are rejected.
 * Selection is driven by the client's request URL path and the provider's configured type.
 */
public abstract class GatewayHandler {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    /** Provider types that speak the OpenAI-compatible Chat Completions API. */
    private static final Set<String> OPENAI_PROVIDER_TYPES = Set.of(
        "vllm",
        "sglang",
        "mindie",
        "mixed-compatible",
        "openai-compatible",
        "deepseek",
        "volcengine",
        "siliconflow",
        "aliyun"
    );

    /** Provider types that speak the Anthropic Messages API. */
    private static final Set<String> ANTHROPIC_PROVIDER_TYPES = Set.of(
        "vllm",
        "mixed-compatible",
        "anthropic-compatible",
        "deepseek",
        "volcengine"
    );

    protected String chunkId;

    // ---- Inner types ----

    /** Result from processing one backend SSE message. */
    public static class StreamResult {
        /** Parsed JSON data extracted from the SSE message, in client format. */
        public ObjectNode data;
        /** SSE message fields to forward to the client via emitter.message(). */
        public ObjectNode sseMessage;
        /** true = standard SSE event format, false = raw JSON line. */
        public boolean isEvent;
        /** Whether this result should be forwarded to the client (false = swallow). */
        public boolean forward = true;

        public static StreamResult forward(ObjectNode data, ObjectNode sseMessage, boolean isEvent) {
            StreamResult r = new StreamResult();
            r.data = data;
            r.sseMessage = sseMessage;
            r.isEvent = isEvent;
            return r;
        }

        public static StreamResult swallow() {
            StreamResult r = new StreamResult();
            r.forward = false;
            return r;
        }
    }

    /** Parsed content and token information extracted from a client-format data node. */
    public static class StreamContent {
        public String content;
        public String reasoning;
        public String toolCallsJson;
        public String finishReason;
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;
        public int cachedPromptTokens;
        public boolean hasError;
    }

    /**
     * Auto-select the appropriate handler based on client request path
     * and the provider's configured type.
     */
    public static GatewayHandler select(HttpServletRequest request, JsonNode provider) {
        boolean clientAnthropic = isAnthropicRequest(request);
        if (clientAnthropic && isAnthropicProvider(provider)) return new AnthropicHandler();
        if (!clientAnthropic && isOpenAIProvider(provider)) return new OpenAIHandler();
        return null;
    }

    /** Detect whether the incoming HTTP request uses the Anthropic /messages format. */
    public static boolean isAnthropicRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.endsWith("/messages");
    }

    /** Detect whether a provider speaks the OpenAI-compatible Chat Completions API. */
    public static boolean isOpenAIProvider(JsonNode provider) {
        String type = provider.at("/type").asText();
        return OPENAI_PROVIDER_TYPES.contains(type);
    }

    /** Detect whether a provider speaks the Anthropic-native API. */
    public static boolean isAnthropicProvider(JsonNode provider) {
        String type = provider.at("/type").asText();
        return ANTHROPIC_PROVIDER_TYPES.contains(type);
    }

    /**
     * Build the HTTP request to send to the backend model provider.
     * Transforms the request body in-place to match the backend API format.
     */
    public HttpRequestBase buildRequest(ObjectNode json, JsonNode model, JsonNode provider, Map<String, String> headers) throws Exception {
        String endpoint = provider.at("/endpoint").asText();
        String token = provider.at("/token").asText();
        String url = buildUrl(endpoint, provider);
        HttpPost request = new HttpPost(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.startsWith("x-") || key.startsWith("anthropic-") || key.startsWith("user-")) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        if (!DPUtil.empty(token)) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        request.addHeader("Content-Type", "application/json;charset=" + CHARSET.name());
        json.put("model", model.at("/name").asText(""));
        json.put("stream", json.at("/stream").asBoolean(false));
        transformRequest(json);
        request.setEntity(new StringEntity(json.toString(), CHARSET));
        chunkId = "chatcmpl-" + DPUtil.random(100000, 999999);
        return request;
    }

    /** Subclass-provided URL path for the backend API. */
    protected abstract String buildUrl(String endpoint, JsonNode provider);

    /** Transform the request body from client format to backend format (in-place). */
    protected abstract void transformRequest(ObjectNode json);

    /**
     * Process one SSE message from the backend.
     * Returns a StreamResult containing the client-format data and the SSE message to forward.
     * Returns a swallowed StreamResult for messages that should not be forwarded.
     */
    public abstract StreamResult processStreamMessage(ObjectNode backendMessage, boolean isEvent);

    /** Process a non-streaming response body from the backend into the client format. */
    public abstract ObjectNode processNonStreamResponse(ObjectNode backendResponse);

    /**
     * Extract content/tokens from a single client-format data node.
     * Returned values are from this message only — the caller manages accumulation.
     */
    public abstract StreamContent extractStreamContent(ObjectNode data);

    // ---- Utility ----

    /** 解析SSE事件消息中的"data"字段 */
    public static ObjectNode parseSseData(ObjectNode message, boolean isEvent) {
        if (!isEvent) {
            JsonNode parsed = DPUtil.parseJSON(DPUtil.stringify(message));
            return parsed instanceof ObjectNode ? (ObjectNode) parsed : DPUtil.objectNode();
        }
        String data = message.at("/data").asText("");
        if (data.startsWith("{")) {
            JsonNode parsed = DPUtil.parseJSON(data);
            return parsed instanceof ObjectNode ? (ObjectNode) parsed : DPUtil.objectNode();
        }
        return DPUtil.objectNode();
    }
}
