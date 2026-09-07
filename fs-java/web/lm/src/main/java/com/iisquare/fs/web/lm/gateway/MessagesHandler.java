package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.lm.entity.Usage;

/**
 * Pass-through handler: Messages client → Messages backend → Messages client.
 */
public class MessagesHandler extends GatewayHandler {

    @Override
    protected String buildUrl(String endpoint, JsonNode provider) {
        if ("deepseek".equals(provider.at("/type").asText())) {
            if (endpoint.endsWith("/v1")) {
                endpoint = endpoint.substring(0, endpoint.length() - 3);
            }
            return endpoint + "/anthropic/v1/messages";
        }
        return endpoint + "/messages";
    }

    @Override
    protected void transformRequest(ObjectNode json) {
        if (!json.has("max_tokens")) {
            json.put("max_tokens", 4096);
        }
    }

    @Override
    public StreamResult processStreamMessage(ObjectNode backendMessage, boolean isEvent) {
        ObjectNode data = parseSseData(backendMessage, isEvent);
        if (data.isEmpty() || "ping".equals(data.at("/type").asText())) {
            return StreamResult.swallow();
        }
        return StreamResult.forward(data, backendMessage, isEvent);
    }

    @Override
    public ObjectNode processNonStreamResponse(ObjectNode backendResponse) {
        return backendResponse;
    }

    @Override
    public String extractPrompt(ObjectNode json, Usage.UsageBuilder usage) {
        StringBuilder sb = new StringBuilder();
        // Anthropic: top-level "system" field (string or array of content blocks)
        String systemContent = extractSystemContent(json);
        if (!systemContent.isEmpty()) {
            sb.append("[system]\n").append(systemContent).append("\n");
        }
        usage.requestSystem(systemContent);
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

    /** Extract Anthropic top-level system content. */
    private String extractSystemContent(ObjectNode json) {
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

    /** Extract Anthropic-format tool use/result blocks from a message. */
    private String extractTools(JsonNode message) {
        StringBuilder sb = new StringBuilder();
        JsonNode content = message.at("/content");
        if (content.isArray()) {
            for (JsonNode block : content) {
                String type = block.at("/type").asText();
                if ("tool_use".equals(type)) {
                    sb.append("tool_use:\n");
                    sb.append("  id: ").append(block.at("/id").asText()).append("\n");
                    sb.append("  name: ").append(block.at("/name").asText()).append("\n");
                    sb.append("  input: ").append(DPUtil.stringify(block.at("/input"))).append("\n");
                } else if ("tool_result".equals(type)) {
                    sb.append("tool_result:\n");
                    sb.append("  tool_use_id: ").append(block.at("/tool_use_id").asText()).append("\n");
                    JsonNode tc = block.at("/content");
                    if (tc.isTextual()) {
                        sb.append("  content: ").append(tc.asText()).append("\n");
                    } else if (tc.isArray()) {
                        for (JsonNode b : tc) {
                            if ("text".equals(b.at("/type").asText())) {
                                sb.append("  content: ").append(b.at("/text").asText()).append("\n");
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public StreamContent extractStreamContent(ObjectNode data) {
        StreamContent c = new StreamContent();
        String type = data.at("/type").asText();
        if ("error".equals(type)) {
            c.hasError = true;
            return c;
        }
        // Type-specific content extraction
        switch (type) {
            case "content_block_delta":
                String deltaType = data.at("/delta/type").asText();
                if ("text_delta".equals(deltaType)) {
                    c.content = data.at("/delta/text").asText();
                } else if ("input_json_delta".equals(deltaType)) {
                    int idx = data.at("/index").asInt();
                    String partial = data.at("/delta/partial_json").asText();
                    ObjectNode chunk = DPUtil.objectNode();
                    chunk.put("index", idx);
                    chunk.putObject("function").put("arguments", partial);
                    c.toolCallsJson = "[" + DPUtil.stringify(chunk) + "]";
                } else if ("thinking_delta".equals(deltaType)) {
                    c.reasoning = data.at("/delta/thinking").asText();
                } else if ("redacted_thinking".equals(deltaType)) {
                    c.reasoning = data.at("/delta/data").asText();
                }
                break;
            case "message_delta":
                c.finishReason = data.at("/delta/stop_reason").asText();
                break;
            case "content_block_start":
                if ("tool_use".equals(data.at("/content_block/type").asText())) {
                    int idx = data.at("/index").asInt();
                    ObjectNode item = DPUtil.objectNode();
                    item.put("index", idx);
                    item.put("id", data.at("/content_block/id").asText());
                    item.put("type", "function");
                    item.putObject("function").put("name", data.at("/content_block/name").asText());
                    c.toolCallsJson = "[" + DPUtil.stringify(item) + "]";
                }
                break;
            case "message":
                // Non-streaming response
                ArrayNode toolCalls = DPUtil.arrayNode();
                for (JsonNode block : data.at("/content")) {
                    String bt = block.at("/type").asText();
                    if ("text".equals(bt)) {
                        c.content = block.at("/text").asText();
                    } else if ("tool_use".equals(bt)) {
                        ObjectNode item = toolCalls.addObject();
                        item.put("index", toolCalls.size() - 1);
                        item.put("id", block.at("/id").asText());
                        item.put("type", "function");
                        ObjectNode func = item.putObject("function");
                        func.put("name", block.at("/name").asText());
                        func.put("arguments", DPUtil.stringify(block.at("/input")));
                    } else if ("thinking".equals(bt)) {
                        c.reasoning = block.at("/thinking").asText();
                    } else if ("redacted_thinking".equals(bt)) {
                        c.reasoning = block.at("/data").asText();
                    }
                }
                if (!toolCalls.isEmpty()) c.toolCallsJson = DPUtil.stringify(toolCalls);
                c.finishReason = data.at("/stop_reason").asText();
                break;
        }
        JsonNode usage = data.at("/usage");
        JsonNode msgUsage = data.at("/message/usage");
        int inputTokens = usage.at("/input_tokens").asInt() + msgUsage.at("/input_tokens").asInt();
        int cacheReadTokens = usage.at("/cache_read_input_tokens").asInt() + msgUsage.at("/cache_read_input_tokens").asInt();
        int cacheCreationTokens = usage.at("/cache_creation_input_tokens").asInt() + msgUsage.at("/cache_creation_input_tokens").asInt();
        c.promptTokens = inputTokens + cacheReadTokens;
        c.completionTokens = usage.at("/output_tokens").asInt();
        c.totalTokens = c.promptTokens + c.completionTokens;
        c.cachedPromptTokens = cacheReadTokens;
        return c;
    }
}
