package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

/**
 * Pass-through handler: Anthropic-format client → Anthropic backend → Anthropic-format client.
 */
public class AnthropicHandler extends GatewayHandler {

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
        c.promptTokens = inputTokens + cacheReadTokens + cacheCreationTokens;
        c.completionTokens = usage.at("/output_tokens").asInt();
        c.totalTokens = c.promptTokens + c.completionTokens;
        c.cachedPromptTokens = cacheReadTokens;
        return c;
    }
}
