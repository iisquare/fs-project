package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Pass-through handler: OpenAI-format client → OpenAI-compatible backend → OpenAI-format client.
 *
 * Minimal transformation — only injects stream_options.include_usage when streaming.
 */
public class OpenAIHandler extends GatewayHandler {

    @Override
    protected String buildUrl(String endpoint, JsonNode provider) {
        return endpoint + "/chat/completions";
    }

    @Override
    protected void transformRequest(ObjectNode json) {
        boolean stream = json.at("/stream").asBoolean(false);
        if (stream) {
            ObjectNode streamOptions = json.has("stream_options")
                ? (ObjectNode) json.at("/stream_options")
                : json.putObject("stream_options");
            streamOptions.put("include_usage", true);
        }
    }

    @Override
    public StreamResult processStreamMessage(ObjectNode backendMessage, boolean isEvent) {
        ObjectNode data = parseSseData(backendMessage, isEvent);
        return StreamResult.forward(data, backendMessage, isEvent);
    }

    @Override
    public ObjectNode processNonStreamResponse(ObjectNode backendResponse) {
        return backendResponse;
    }

    @Override
    public StreamContent extractStreamContent(ObjectNode data) {
        StreamContent c = new StreamContent();
        if (data.has("error")) {
            c.hasError = true;
            return c;
        }
        // Extract from choices[].delta (stream) or choices[].message (non-stream)
        for (int i = 0; i < data.at("/choices").size(); i++) {
            ObjectNode item = (ObjectNode) data.at("/choices/" + i);
            boolean isDelta = item.has("delta");
            String reasoning = (isDelta ? item.at("/delta") : item.at("/message")).at("/reasoning_content").asText(null);
            String content = (isDelta ? item.at("/delta") : item.at("/message")).at("/content").asText(null);
            if (reasoning != null) c.reasoning = reasoning;
            if (content != null) c.content = content;
            if (item.has("finish_reason")) {
                c.finishReason = item.at("/finish_reason").asText();
            }
            JsonNode toolCalls = (isDelta ? item.at("/delta") : item.at("/message")).get("tool_calls");
            if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                c.toolCallsJson = toolCalls.toPrettyString();
            }
        }
        if (data.has("usage")) {
            c.promptTokens = data.at("/usage/prompt_tokens").asInt();
            c.completionTokens = data.at("/usage/completion_tokens").asInt();
            c.totalTokens = data.at("/usage/total_tokens").asInt();
            c.cachedPromptTokens = data.at("/usage/prompt_tokens_details/cached_tokens").asInt();
        }
        return c;
    }
}
