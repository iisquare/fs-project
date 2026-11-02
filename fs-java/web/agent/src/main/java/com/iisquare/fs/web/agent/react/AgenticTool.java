package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ReAct / FunctionCalling 工具 - 大语言模型可调用的能力，按类型一个实现类：
 * - method：工具方法（外部调用，只带工具自身配置的请求头）
 * - knowledge：知识库（内部调用，按召回范围取结果）
 * - theme：数据主题（内部调用，先取数据字典再执行查询，透传登录用户）
 * - agentic：编排应用（内部调用，把另一个编排当工具）
 *
 * 函数名由工具实现生成，参数以 OpenAI function calling 的 schema 暴露给模型；
 * 模型可见的描述统一由 ToolDescriptionService 在注册中心写入。
 */
public interface AgenticTool {

    /** 工具类型：与 LLM 节点 tools[].kind 对应 */
    String kind();

    /**
     * 生成 function calling 定义
     * @param item 编排里配置的工具项（含 args 执行变量绑定）
     * @return 定义节点；返回 null 表示当前配置不可用（跳过该工具）
     */
    ObjectNode definition(JsonNode item);

    /**
     * 执行工具调用
     * @param runtime 运行时：手工绑定的执行变量需要按变量解析取值
     * @param item    编排里配置的工具项
     * @param args    模型给出的调用参数
     * @param outputs 已执行节点的输出（解析执行变量里的引用）
     * @param variables 会话变量（解析执行变量里的引用）
     */
    Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                  Map<String, ObjectNode> outputs, Map<String, Object> variables) throws Exception;

    /**
     * 通用 function 定义：参数按名展开，整只被手工绑定的参数不暴露给模型。
     * description 不在这里生成，由 AgenticToolService 统一写入。
     */
    static ObjectNode definition(String name, ArrayNode params, JsonNode binding) {
        ObjectNode function = DPUtil.objectNode();
        function.put("name", name);
        ObjectNode properties = function.putObject("parameters").put("type", "object").putObject("properties");
        ArrayNode required = DPUtil.arrayNode();
        if (null != params) {
            for (JsonNode parameter : params) {
                String paramName = parameter.at("/name").asText("");
                if (DPUtil.empty(paramName)) continue;
                JsonNode item = binding.at("/args/" + paramName);
                // 普通参数：整只被手工绑定后不再暴露给模型
                if (!item.has("fields") && manual(binding, paramName)) continue;
                ObjectNode schema = parameter.has("schema")
                        ? (ObjectNode) parameter.at("/schema").deepCopy()
                        : schema("string", "");
                // OpenAPI/MCP 的说明可能夹带整段文档与示例；统一收敛后再交给模型。
                sanitizeSchema(schema, 0);
                // 对象参数（如 body）：手工字段从 schema 里摘掉（由编排覆盖），其余字段交给模型填写
                if (item.has("fields")) {
                    ObjectNode fields = (ObjectNode) schema.at("/properties");
                    if (null != fields && fields.isObject()) {
                        List<String> manualFields = new ArrayList<>();
                        fields.fieldNames().forEachRemaining(field -> {
                            JsonNode fieldBinding = item.at("/fields/" + field);
                            if (fieldBinding.has("auto") && false == fieldBinding.at("/auto").asBoolean(true)) {
                                manualFields.add(field);
                            }
                        });
                        manualFields.forEach(fields::remove);
                        if (fields.isEmpty()) continue; // 字段全部手工：模型无需填写
                        removeRequired(schema, manualFields);
                    }
                }
                properties.set(paramName, schema);
                if (parameter.at("/required").asBoolean(false)) required.add(paramName);
            }
        }
        if (!required.isEmpty()) ((ObjectNode) function.at("/parameters")).set("required", required);
        ObjectNode tool = DPUtil.objectNode();
        tool.put("type", "function");
        tool.set("function", function);
        return tool;
    }

    /** schema 里去掉手工绑定的字段后，required 列表同步收敛 */
    static void removeRequired(ObjectNode schema, List<String> manualFields) {
        JsonNode list = schema.at("/required");
        if (!list.isArray()) return;
        ArrayNode next = DPUtil.arrayNode();
        for (JsonNode item : list) {
            if (manualFields.contains(item.asText(""))) continue;
            next.add(item);
        }
        if (next.isEmpty()) schema.remove("required");
        else schema.set("required", next);
    }

    /** 该参数是否已由编排手工指定：指定后不再交给模型填写 */
    static boolean manual(JsonNode binding, String name) {
        JsonNode item = binding.at("/args/" + name);
        if (item.has("auto")) return false == item.at("/auto").asBoolean(true);
        return item.has("fields");
    }

    /** 参数 schema：类型 + 说明 */
    static ObjectNode schema(String type, String description) {
        ObjectNode schema = DPUtil.objectNode();
        schema.put("type", type);
        if (!DPUtil.empty(description)) schema.put("description", description);
        return schema;
    }

    /**
     * 参数 schema 的说明同样属于模型可见工具描述：
     * 保留操作者填写的完整说明，去掉厂商扩展、注释和示例，保留调用所需的类型、约束与枚举。
     */
    static void sanitizeSchema(ObjectNode schema, int depth) {
        if (null == schema || depth > 8) return;
        schema.remove("examples");
        schema.remove("$comment");
        for (Iterator<String> iterator = schema.fieldNames(); iterator.hasNext(); ) {
            String field = iterator.next();
            if (field.startsWith("x-")) {
                iterator.remove();
                continue;
            }
            if ("description".equals(field) && schema.get(field).isTextual()) {
                schema.put(field, cleanSchemaText(schema.get(field).asText("")));
            } else if ("title".equals(field) && schema.get(field).isTextual()) {
                schema.put(field, cleanSchemaText(schema.get(field).asText("")));
            }
        }
        recurseSchema(schema.at("/properties"), depth);
        recurseSchema(schema.at("/$defs"), depth);
        recurseSchema(schema.at("/items"), depth);
        recurseSchema(schema.at("/additionalProperties"), depth);
        recurseSchema(schema.at("/oneOf"), depth);
        recurseSchema(schema.at("/anyOf"), depth);
        recurseSchema(schema.at("/allOf"), depth);
    }

    static void recurseSchema(JsonNode node, int depth) {
        if (null == node || node.isMissingNode() || node.isNull()) return;
        if (node.isObject()) {
            ObjectNode value = (ObjectNode) node;
            if (value.has("type") || value.has("properties") || value.has("items")
                    || value.has("$ref") || value.has("oneOf") || value.has("anyOf") || value.has("allOf")) {
                sanitizeSchema(value, depth + 1);
                return;
            }
            value.fields().forEachRemaining(entry -> recurseSchema(entry.getValue(), depth + 1));
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) recurseSchema(item, depth + 1);
        }
    }

    static String cleanSchemaText(String value) {
        return DPUtil.parseString(value).trim();
    }

    /** 参数项：名称 + 是否必填 + schema */
    static ObjectNode parameter(String name, boolean required, ObjectNode schema) {
        ObjectNode parameter = DPUtil.objectNode();
        parameter.put("name", name);
        parameter.put("required", required);
        parameter.set("schema", schema);
        return parameter;
    }

}
