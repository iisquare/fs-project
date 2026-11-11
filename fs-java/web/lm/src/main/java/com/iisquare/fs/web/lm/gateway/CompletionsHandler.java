package com.iisquare.fs.web.lm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.lm.entity.Usage;

import java.util.Locale;
import java.util.Set;

/**
 * Chat Completions client → provider-specific backend → normalized Chat Completions client.
 *
 * Keeps the public boundary OpenAI-compatible while adapting provider reasoning controls.
 */
public class CompletionsHandler extends GatewayHandler {

    private static final Set<String> TEMPLATE_KWARG_PROVIDERS = Set.of("vllm", "sglang");

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

    /**
     * Callers of /v1/chat/completions always use the OpenAI contract.
     * Provider-specific thinking switches are translated here, so agent
     * services never need to know which inference server is configured.
     */
    @Override
    protected void transformRequest(ObjectNode json, JsonNode model, JsonNode provider) {
        transformRequest(json);
        String providerType = provider.at("/type").asText("").toLowerCase(Locale.ROOT);
        if (TEMPLATE_KWARG_PROVIDERS.contains(providerType)) {
            // 新调用方使用 OpenAI reasoning_effort；旧调用方可能仍传 thinking 对象。
            // 旧 agent 可能在关闭思考时仍带 reasoning_effort，因此显式 thinking 优先，
            // 否则关闭开关会被强度字段重新解释成开启。
            if (translateLegacyThinking(json, model)) return;
            translateReasoningEffort(json, model);
            return;
        }
        if ("deepseek".equals(providerType)) translateDeepSeekReasoningEffort(json);
    }

    protected void translateReasoningEffort(ObjectNode json, JsonNode model) {
        JsonNode effortNode = json.get("reasoning_effort");
        if (null == effortNode || !effortNode.isTextual()) return;
        String effort = effortNode.asText("").trim().toLowerCase(Locale.ROOT);
        if (effort.isEmpty()) return;
        json.put("reasoning_effort", effort);
        JsonNode current = json.get("chat_template_kwargs");
        if (null != current && !current.isObject()) return; // 自定义参数非法时交给供应商报错，不静默覆盖
        ObjectNode kwargs = null != current ? (ObjectNode) current : json.putObject("chat_template_kwargs");
        String key = thinkingTemplateKey(model.at("/name").asText(""));
        if (!kwargs.has(key)) kwargs.put(key, !"none".equals(effort));
    }

    protected boolean translateLegacyThinking(ObjectNode json, JsonNode model) {
        JsonNode thinking = json.get("thinking");
        if (null == thinking || !thinking.isObject()) return false;
        String type = thinking.at("/type").asText("").toLowerCase(Locale.ROOT);
        if (!"enabled".equals(type) && !"disabled".equals(type)) return false;
        JsonNode current = json.get("chat_template_kwargs");
        if (null != current && !current.isObject()) return false;
        ObjectNode kwargs = null != current ? (ObjectNode) current : json.putObject("chat_template_kwargs");
        String key = thinkingTemplateKey(model.at("/name").asText(""));
        if (!kwargs.has(key)) kwargs.put(key, "enabled".equals(type));
        if ("disabled".equals(type)) json.put("reasoning_effort", "none");
        // Chat Completions 没有通用的 thinking 对象；转换后移除，避免严格校验的
        // vLLM/SGLang 版本把未知字段直接判为 400。
        json.remove("thinking");
        return true;
    }

    /** DeepSeek 的 OpenAI 兼容接口使用 thinking 对象，不接受通用 reasoning_effort。 */
    protected void translateDeepSeekReasoningEffort(ObjectNode json) {
        JsonNode legacyThinking = json.get("thinking");
        if (null != legacyThinking && legacyThinking.isObject()) {
            String type = legacyThinking.at("/type").asText("").toLowerCase(Locale.ROOT);
            if ("enabled".equals(type) || "disabled".equals(type)) {
                json.remove("reasoning_effort");
                return;
            }
        }
        JsonNode effortNode = json.get("reasoning_effort");
        if (null == effortNode || !effortNode.isTextual()) return;
        String effort = effortNode.asText("").trim().toLowerCase(Locale.ROOT);
        if (effort.isEmpty()) return;
        ObjectNode thinking = json.has("thinking") && json.at("/thinking").isObject()
                ? (ObjectNode) json.at("/thinking")
                : json.putObject("thinking");
        thinking.put("type", "none".equals(effort) ? "disabled" : "enabled");
        json.remove("reasoning_effort");
    }

    /**
     * DeepSeek-V3.x keeps its historical thinking key. Other current
     * vLLM/SGLang models use enable_thinking; special legacy model families
     * are intentionally not given one-off compatibility branches.
     */
    protected String thinkingTemplateKey(String model) {
        String name = null == model ? "" : model.toLowerCase(Locale.ROOT).replace('_', '-');
        if (name.contains("deepseek-v3.1") || name.contains("deepseek-v3.2")) {
            return "thinking";
        }
        return "enable_thinking";
    }

    @Override
    public StreamResult processStreamMessage(ObjectNode backendMessage, boolean isEvent) {
        ObjectNode data = parseSseData(backendMessage, isEvent);
        normalizeReasoning(data);
        // Forward even if data is empty — [DONE] markers produce empty parsed data
        // but must reach the client as raw SSE events (sseMessage in StreamResult).
        return StreamResult.forward(data, backendMessage, isEvent);
    }

    @Override
    public ObjectNode processNonStreamResponse(ObjectNode backendResponse) {
        normalizeReasoning(backendResponse);
        return backendResponse;
    }

    /**
     * vLLM的最新 reasoning parser 使用 choice.message/delta.reasoning，早期版本和
     * SGLang 使用 reasoning_content。agent 现有协议读取后者，因此在网关边界补齐别名。
     */
    protected void normalizeReasoning(ObjectNode data) {
        if (null == data) return;
        for (JsonNode choice : data.at("/choices")) {
            normalizeReasoning(choice.at("/delta"));
            normalizeReasoning(choice.at("/message"));
        }
    }

    protected void normalizeReasoning(JsonNode source) {
        if (null == source || !source.isObject()) return;
        String reasoning = source.at("/reasoning").asText("");
        String legacy = source.at("/reasoning_content").asText("");
        if (!reasoning.isEmpty() && legacy.isEmpty()) {
            ((ObjectNode) source).put("reasoning_content", reasoning);
        }
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
            JsonNode source = isDelta ? item.at("/delta") : item.at("/message");
            String reasoning = source.at("/reasoning_content").asText(null);
            if (reasoning == null || reasoning.isEmpty()) {
                reasoning = source.at("/reasoning").asText(null);
            }
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
