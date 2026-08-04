package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.lm.entity.Usage;

/**
 * Pass-through handler: Responses API client → Responses API backend → Responses API client.
 *
 * Currently only DeepSeek provider supports the OpenAI-compatible Responses API.
 * The request/response body is passed through with minimal transformation —
 * the gateway handles auth, rate-limiting, credit billing, and sensitive-word
 * filtering around the passthrough.
 *
 * @see <a href="https://api.deepseek.com/docs">DeepSeek Responses API</a>
 */
public class ResponsesHandler extends GatewayHandler {

    @Override
    protected String buildUrl(String endpoint, JsonNode provider) {
        return endpoint + "/responses";
    }

    @Override
    protected void transformRequest(ObjectNode json) {
        // No significant transformation needed.
        // DeepSeek supports the OpenAI Responses API format natively.
        // The base buildRequest() already injects "model" and "stream".
    }

    @Override
    public StreamResult processStreamMessage(ObjectNode backendMessage, boolean isEvent) {
        ObjectNode data = parseSseData(backendMessage, isEvent);
        if (data.isEmpty()) {
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
        // Extract top-level instructions (system message equivalent)
        String instructions = json.at("/instructions").asText("");
        if (!instructions.isEmpty()) {
            sb.append("[instructions]\n").append(instructions).append("\n");
        }
        usage.requestSystem(instructions);
        // Extract input text — single pass over the array
        JsonNode input = json.at("/input");
        if (input.isTextual()) {
            String text = input.asText();
            sb.append("[user]\n").append(text).append("\n");
            usage.requestUser(text);
        } else if (input.isArray()) {
            String lastUserContent = "";
            for (JsonNode item : input) {
                String itemType = item.at("/type").asText();
                if ("message".equals(itemType)) {
                    String role = item.at("/role").asText();
                    if ("system".equals(role) || "developer".equals(role)) {
                        // Capture inline system content for tracking if top-level instructions absent
                        String sysContent = extractResponsesMessageContent(item);
                        if (instructions.isEmpty() && !sysContent.isEmpty()) {
                            instructions = sysContent;
                            usage.requestSystem(instructions);
                        }
                        // Append system message to prompt text
                        if (!sysContent.isEmpty()) {
                            sb.append("[").append(role).append("]\n").append(sysContent).append("\n");
                        }
                        continue;
                    }
                    JsonNode content = item.at("/content");
                    if (content.isTextual()) {
                        String text = content.asText();
                        sb.append("[").append(role).append("]\n").append(text).append("\n");
                        if ("user".equals(role)) lastUserContent = text;
                    } else if (content.isArray()) {
                        for (JsonNode block : content) {
                            String blockType = block.at("/type").asText();
                            if ("input_text".equals(blockType) || "output_text".equals(blockType)) {
                                String text = block.at("/text").asText();
                                sb.append("[").append(role).append("]\n").append(text).append("\n");
                                if ("user".equals(role)) lastUserContent = text;
                            }
                        }
                    }
                } else if ("function_call".equals(itemType)) {
                    sb.append("[assistant]\nfunction_call: ").append(item.at("/name").asText());
                    sb.append("(").append(item.at("/arguments").asText()).append(")\n");
                } else if ("function_call_output".equals(itemType)) {
                    sb.append("[tool]\n");
                    sb.append("  call_id: ").append(item.at("/call_id").asText()).append("\n");
                    sb.append("  output: ").append(item.at("/output").asText()).append("\n");
                } else if ("reasoning".equals(itemType)) {
                    sb.append("[assistant]\nreasoning: ");
                    sb.append(item.at("/content").asText()).append("\n");
                } else if ("item_reference".equals(itemType)) {
                    sb.append("[item_reference] id: ").append(item.at("/id").asText()).append("\n");
                } else {
                    sb.append("[").append(itemType).append("]\n");
                }
            }
            usage.requestUser(lastUserContent);
        }
        String prompt = sb.toString();
        usage.requestPrompt(prompt);
        return prompt;
    }

    @Override
    public StreamContent extractStreamContent(ObjectNode data) {
        StreamContent c = new StreamContent();

        // Top-level error — only treat non-null error objects as actual errors.
        // The OpenAI Responses API includes "error": null in normal responses,
        // so a plain has("error") would false-positive.
        if (data.has("error") && !data.get("error").isNull()) {
            c.hasError = true;
            return c;
        }

        String type = data.at("/type").asText();

        // Error embedded in a response.* event
        if (type.startsWith("response.") && data.at("/response/error").isObject()) {
            c.hasError = true;
            return c;
        }

        // SSE event types intentionally passed through without content extraction:
        //   response.created, response.in_progress, response.content_part.added,
        //   response.function_call_arguments.done, response.output_item.done
        // These carry no incremental content/tokens — they are forwarded as-is.
        switch (type) {
            case "response.output_text.delta":
                c.content = data.at("/delta").asText();
                break;
            case "response.output_text.done":
                c.content = data.at("/text").asText();
                break;
            case "response.reasoning_text.delta":
                c.reasoning = data.at("/delta").asText();
                break;
            case "response.reasoning_text.done":
                c.reasoning = data.at("/text").asText();
                break;
            case "response.refusal_delta":
                c.content = data.at("/delta").asText();
                break;
            case "response.refusal_done":
                c.content = data.at("/text").asText();
                break;
            case "response.output_item.added":
                // Emit tool call metadata in the format mergeToolCallChunk expects
                if ("function_call".equals(data.at("/item/type").asText())) {
                    int idx = data.at("/output_index").asInt();
                    ObjectNode item = DPUtil.objectNode();
                    item.put("index", idx);
                    item.put("id", data.at("/item/id").asText());
                    item.put("type", "function");
                    item.putObject("function").put("name", data.at("/item/name").asText());
                    c.toolCallsJson = "[" + DPUtil.stringify(item) + "]";
                }
                break;
            case "response.function_call_arguments.delta":
                {
                    // Wrap delta in the array format mergeToolCallChunk expects:
                    // [{index: N, function: {arguments: "partial_json"}}]
                    int idx = data.at("/output_index").asInt();
                    String delta = data.at("/delta").asText();
                    ObjectNode chunk = DPUtil.objectNode();
                    chunk.put("index", idx);
                    chunk.putObject("function").put("arguments", delta);
                    c.toolCallsJson = "[" + DPUtil.stringify(chunk) + "]";
                }
                break;
            case "response.completed":
            case "response.incomplete":
                // Extract usage from the response object
                {
                    JsonNode usage = data.at("/response/usage");
                    if (!usage.isMissingNode()) {
                        c.promptTokens = usage.at("/input_tokens").asInt();
                        c.completionTokens = usage.at("/output_tokens").asInt();
                        c.totalTokens = usage.at("/total_tokens").asInt();
                        c.cachedPromptTokens = usage.at("/input_tokens_details/cached_tokens").asInt();
                    }
                }
                // Extract finish reason, errors, and tool calls from output items.
                // For streaming this event arrives after all deltas, so we only
                // extract metadata here — content/text is already accumulated.
                extractOutputMeta(data.at("/response/output"), c);
                // Fallback to response-level status if no output item provided finishReason
                if (c.finishReason == null) {
                    String status = data.at("/response/status").asText(null);
                    if (status != null && !status.isEmpty()) {
                        c.finishReason = status;
                    }
                }
                // Incomplete response — capture the truncation reason (overrides status)
                if ("response.incomplete".equals(type)) {
                    String reason = data.at("/response/incomplete_details/reason").asText();
                    if (!reason.isEmpty()) {
                        c.finishReason = reason;
                    }
                }
                break;
            case "response.failed":
                c.hasError = true;
                break;
        }

        // Handle non-streaming flat response (no "type" wrapper at top level).
        // Some providers may return the Responses API response without the SSE
        // event envelope, with output/usage directly at the top level.
        if (type.isEmpty()) {
            JsonNode usage = data.at("/usage");
            if (!usage.isMissingNode()) {
                c.promptTokens = usage.at("/input_tokens").asInt();
                c.completionTokens = usage.at("/output_tokens").asInt();
                c.totalTokens = usage.at("/total_tokens").asInt();
                c.cachedPromptTokens = usage.at("/input_tokens_details/cached_tokens").asInt();
            }
            // Full extraction — non-streaming, so we need content and metadata
            extractOutputItems(data.at("/output"), c);
            // Fallback to top-level status
            if (c.finishReason == null) {
                String status = data.at("/status").asText(null);
                if (status != null && !status.isEmpty()) {
                    c.finishReason = status;
                }
            }
        }

        return c;
    }

    /**
     * Extract text content from a Responses-format message node.
     * Handles both plain-string content and content-block arrays ({@code input_text} blocks).
     */
    private static String extractResponsesMessageContent(JsonNode message) {
        JsonNode content = message.at("/content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : content) {
                if ("input_text".equals(block.at("/type").asText())) {
                    sb.append(block.at("/text").asText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * Extract finish reason and errors from an output array (streaming path).
     * Does NOT extract text content, reasoning, or tool calls — those are already
     * accumulated from delta events ({@code response.output_text.delta},
     * {@code response.reasoning_text.delta}, {@code response.function_call_arguments.delta}).
     * Use {@link #extractOutputItems} for the non-streaming path.
     */
    private void extractOutputMeta(JsonNode output, StreamContent c) {
        if (!output.isArray()) return;
        for (JsonNode item : output) {
            String oType = item.at("/type").asText();
            switch (oType) {
                case "message":
                    if (c.finishReason == null) {
                        c.finishReason = item.at("/status").asText();
                    }
                    break;
                case "error":
                    c.hasError = true;
                    break;
                case "reasoning":
                    // Only set if not already populated from delta events
                    if (c.reasoning == null) {
                        c.reasoning = item.at("/summary").asText();
                    }
                    break;
                // function_call is intentionally NOT extracted here —
                // tool call metadata and arguments are accumulated from
                // response.output_item.added and response.function_call_arguments.delta
            }
        }
    }

    /** Extract content, reasoning, tool calls, finish reason, and errors from an output array. */
    private void extractOutputItems(JsonNode output, StreamContent c) {
        if (!output.isArray()) return;
        ArrayNode toolCalls = DPUtil.arrayNode();
        int functionCallCount = 0;
        for (int i = 0; i < output.size(); i++) {
            JsonNode item = output.get(i);
            String oType = item.at("/type").asText();
            switch (oType) {
                case "message":
                    if (c.finishReason == null) {
                        c.finishReason = item.at("/status").asText();
                    }
                    // Extract text — content can be a plain string or an array of blocks
                    {
                        JsonNode content = item.at("/content");
                        if (content.isTextual()) {
                            c.content = content.asText();
                        } else if (content.isArray()) {
                            StringBuilder textBuilder = new StringBuilder();
                            for (JsonNode block : content) {
                                if ("output_text".equals(block.at("/type").asText())) {
                                    textBuilder.append(block.at("/text").asText());
                                }
                            }
                            c.content = textBuilder.toString();
                        }
                    }
                    break;
                case "error":
                    c.hasError = true;
                    break;
                case "function_call":
                    {
                        // Items in response.output may lack output_index; fall back to
                        // a 0-based counter among function_call items in this array.
                        int idx = item.at("/output_index").isMissingNode() ? functionCallCount : item.at("/output_index").asInt();
                        ObjectNode tc = toolCalls.addObject();
                        tc.put("index", idx);
                        tc.put("id", item.at("/id").asText());
                        tc.put("type", "function");
                        ObjectNode func = tc.putObject("function");
                        func.put("name", item.at("/name").asText());
                        func.put("arguments", item.at("/arguments").asText());
                        functionCallCount++;
                    }
                    break;
                case "reasoning":
                    c.reasoning = item.at("/summary").asText();
                    break;
            }
        }
        if (!toolCalls.isEmpty()) {
            c.toolCallsJson = DPUtil.stringify(toolCalls);
        }
    }
}
