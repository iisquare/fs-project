package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.web.lm.entity.Usage;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract handler for LLM gateway request forwarding.
 * Three concrete implementations handle pass-through forwarding:
 *   Chat Completions client → Chat Completions backend,
 *   Messages client → Messages backend,
 *   Responses API client → Responses API backend.
 * Cross-format mismatches (e.g. Messages client → Chat Completions backend) are rejected.
 * Selection is driven by the client's request URL path and the provider's configured type.
 */
public abstract class GatewayHandler {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final List<String> forwardHeaderPrefixes = Arrays.asList(
            "x-",
            "anthropic-",
            "user-",
            "session-",
            "thread-"
    );

    public static final List<String> forwardHeaderBlocks = Arrays.asList(
            "authorization",
            "x-api-key"
    );

    public static final List<String> forwardedResponsePrefixes = Arrays.asList( // 回传给调用端的响应头
            "x-"
    );

    /** Provider types that speak the Chat Completions API. */
    private static final Set<String> COMPLETIONS_PROVIDER_TYPES = Set.of(
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

    /** Provider types that speak the Messages API. */
    private static final Set<String> MESSAGES_PROVIDER_TYPES = Set.of(
        "vllm",
        "mixed-compatible",
        "anthropic-compatible",
        "deepseek",
        "volcengine"
    );

    /** Provider types that speak the Responses API. */
    private static final Set<String> RESPONSES_PROVIDER_TYPES = Set.of(
        "deepseek"
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
        if (isResponsesRequest(request) && isResponsesProvider(provider)) return new ResponsesHandler();
        if (isMessagesRequest(request) && isMessagesProvider(provider)) return new MessagesHandler();
        if (!isMessagesRequest(request) && !isResponsesRequest(request) && isCompletionsProvider(provider)) return new CompletionsHandler();
        return null;
    }

    /** Detect whether the incoming HTTP request uses the Messages API format. */
    public static boolean isMessagesRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.endsWith("/messages");
    }

    /** Detect whether a provider speaks the Chat Completions API. */
    public static boolean isCompletionsProvider(JsonNode provider) {
        String type = provider.at("/type").asText();
        return COMPLETIONS_PROVIDER_TYPES.contains(type);
    }

    /** Detect whether a provider speaks the Messages API. */
    public static boolean isMessagesProvider(JsonNode provider) {
        String type = provider.at("/type").asText();
        return MESSAGES_PROVIDER_TYPES.contains(type);
    }

    /** Detect whether the incoming HTTP request uses the Responses API format. */
    public static boolean isResponsesRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.endsWith("/responses");
    }

    /** Detect whether a provider speaks the Responses API. */
    public static boolean isResponsesProvider(JsonNode provider) {
        String type = provider.at("/type").asText();
        return RESPONSES_PROVIDER_TYPES.contains(type);
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
            if (forwardHeaderBlocks.contains(key)) continue;
            for (String prefix : forwardHeaderPrefixes) {
                if (key.startsWith(prefix)) {
                    request.addHeader(key, entry.getValue());
                    break;
                }
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
        chunkId = "gw-" + DPUtil.random(100000, 999999);
        return request;
    }

    public SsePlainEmitter forwardedResponseHeaders(SsePlainEmitter emitter, CloseableHttpResponse response) {
        try {
            emitter.setMediaType(response); // 需要在异步返回前，确定请求响应类型
            for (Header header : response.getAllHeaders()) {
                String name = header.getName().toLowerCase();
                for (String prefix : forwardedResponsePrefixes) {
                    if (name.startsWith(prefix)) {
                        emitter.response.addHeader(name, header.getValue());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
        return emitter;
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

    /**
     * Extract prompt text from the request body for sensitive-word checking and usage logging.
     * Each handler knows its own protocol format (Chat Completions, Messages, or Responses).
     */
    public abstract String extractPrompt(ObjectNode json, Usage.UsageBuilder usage);

    // ---- Shared utilities ----

    /** Extract text content from a message node, handling both string and content-block-array formats. */
    protected static String extractMessageContent(JsonNode message) {
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
