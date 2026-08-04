package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.lm.entity.Usage;

/**
 * Pass-through handler: Chat Completions client → Chat Completions backend → Chat Completions client.
 *
 * Minimal transformation — only injects stream_options.include_usage when streaming.
 */
public class CompletionsHandler extends GatewayHandler {

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
        // Forward even if data is empty — [DONE] markers produce empty parsed data
        // but must reach the client as raw SSE events (sseMessage in StreamResult).
        return StreamResult.forward(data, backendMessage, isEvent);
    }

    @Override
    public ObjectNode processNonStreamResponse(ObjectNode backendResponse) {
        return backendResponse;
    }

    @Override
    public String extractPrompt(ObjectNode json, Usage.UsageBuilder usage) {
        StringBuilder sb = new StringBuilder();
        String systemContent = "";
        String lastUserContent = "";
        for (JsonNode message : json.at("/messages")) {
            String role = message.at("/role").asText();
            String content = extractMessageContent(message);
            String tools = extractTools(message);
            sb.append("[").append(role).append("]\n").append(content);
            if (!tools.isEmpty()) sb.append("\n").append(tools);
            sb.append("\n");
            if ("system".equals(role) && systemContent.isEmpty()) {
                systemContent = content;
            }
            if ("user".equals(role)) {
                lastUserContent = content;
            }
        }
        usage.requestSystem(systemContent);
        usage.requestUser(lastUserContent);
        String prompt = sb.toString();
        usage.requestPrompt(prompt);
        return prompt;
    }

    /** Extract OpenAI-format tool calls from a message. */
    private String extractTools(JsonNode message) {
        StringBuilder sb = new StringBuilder();
        JsonNode toolCalls = message.at("/tool_calls");
        if (toolCalls.isArray() && !toolCalls.isEmpty()) {
            for (JsonNode tc : toolCalls) {
                sb.append("tool_calls:\n");
                sb.append("  id: ").append(tc.at("/id").asText()).append("\n");
                sb.append("  function: ").append(tc.at("/function/name").asText()).append("\n");
                sb.append("  arguments: ").append(tc.at("/function/arguments").asText()).append("\n");
            }
        }
        if ("tool".equals(message.at("/role").asText())) {
            sb.append("[tool]\n");
            sb.append("  tool_call_id: ").append(message.at("/tool_call_id").asText()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public StreamContent extractStreamContent(ObjectNode data) {
        StreamContent c = new StreamContent();
        if (data.has("error") && !data.get("error").isNull()) {
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
            if (item.has("finish_reason") && !item.get("finish_reason").isNull()) {
                c.finishReason = item.at("/finish_reason").asText();
            }
            JsonNode toolCalls = (isDelta ? item.at("/delta") : item.at("/message")).get("tool_calls");
            if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                c.toolCallsJson = DPUtil.stringify(toolCalls);
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
