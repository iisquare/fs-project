package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具说明：统一维护模型可见的 function description。
 *
 * 编排的 LLM 节点和客户端对话都从这里取同一份说明，避免工具实现里各自拼描述。
 * 固定模板只交代模型选工具所需的「能力与适用场景」；工具方法与编排应用可再附业务描述。
 * 知识库、数据主题、知识图谱的业务描述不在定义阶段下发，而是在实际调用结果中返回。
 * 建议说明聚焦能力、适用场景和关键约束；操作者确需写详细内容时保留全文，不做字数截断。
 * /bot/tools 返回渲染后的最终说明，客户端下一次新建会话即可生效；
 * /bot/tool-descriptions 提供模板，供工具目录、帮助展示或兼容使用。
 */
@Service
public class ToolDescriptionService {

    /** 模板版本：说明策略有不兼容变化时递增，客户端本次只按会话刷新，不强制使用该值 */
    public static final String VERSION = "4";
    private static final String NAME = "{name}";
    private static final String DEFAULT_LABEL = "项目工具";
    private static final String DEFAULT_TEMPLATE = "用于调用项目工具「" + NAME + "」并获取结果。";
    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

    static {
        put("method", "工具方法", "调用工具方法「" + NAME + "」并返回结果。");
        put("knowledge", "知识库", "检索知识库「" + NAME + "」中的相关内容，用于回答该知识领域内的问题。");
        put("theme", "数据主题", "查询数据主题「" + NAME + "」：可返回数据集与字段字典，也可执行只读查询。");
        put("ontology", "知识图谱", "查询知识图谱「" + NAME + "」：检索实体，或推理两个实体之间的关联路径。");
        put("agentic", "编排应用", "将具体任务或问题交给编排应用「" + NAME + "」处理并返回结果。");
    }

    private static void put(String kind, String label, String template) {
        LABELS.put(kind, label);
        TEMPLATES.put(kind, template);
    }

    /** 客户端可用的说明模板清单：kind + 展示类型 + 含 {name} 占位符的描述 */
    public ArrayNode definitions() {
        ArrayNode rows = DPUtil.arrayNode();
        for (Map.Entry<String, String> entry : TEMPLATES.entrySet()) {
            String kind = entry.getKey();
            ObjectNode row = rows.addObject();
            row.put("kind", kind);
            row.put("label", LABELS.getOrDefault(kind, DEFAULT_LABEL));
            row.put("description", entry.getValue());
        }
        return rows;
    }

    /** 按工具类型和名称渲染模型可见说明 */
    public String description(String kind, String name) {
        return description(kind, name, "");
    }

    /** 按工具类型、名称和简短用途渲染模型可见说明 */
    public String description(String kind, String name, String detail) {
        String template = TEMPLATES.getOrDefault(normalizeKind(kind), DEFAULT_TEMPLATE);
        String base = template.replace(NAME, DPUtil.parseString(name));
        if (DPUtil.empty(detail)) return base;
        String suffix = sentence(detail);
        if ("method".equals(normalizeKind(kind))) {
            return "工具方法「" + DPUtil.parseString(name) + "」：" + suffix;
        }
        return base + suffix;
    }

    /**
     * 统一覆盖 function description：所有工具实现都经过这里，后续新增工具类型也不会绕过。
     */
    public ObjectNode apply(ObjectNode definition, JsonNode item) {
        if (null == definition) return null;
        JsonNode function = definition.at("/function");
        if (!function.isObject()) return definition;
        String functionName = function.at("/name").asText("");
        String kind = kindOf(item);
        String name = name(item, functionName);
        ((ObjectNode) function).put("description", description(kind, name, detail(item, kind, name)));
        return definition;
    }

    protected String kindOf(JsonNode item) {
        String kind = null == item ? "" : item.at("/kind").asText("method");
        return DPUtil.empty(kind) ? "method" : kind;
    }

    protected String normalizeKind(String kind) {
        return DPUtil.empty(kind) ? "method" : kind;
    }

    /**
     * 工具方法优先使用「工具名 / 方法标题」，其余工具优先使用对象展示名。
     * function name 只用于调用路由，不作为模型阅读的首选名称。
     */
    protected String name(JsonNode item, String functionName) {
        if ("method".equals(kindOf(item))) {
            String toolName = text(item, "toolName");
            String title = text(item, "methodTitle");
            if (DPUtil.empty(title)) title = text(item, "title");
            if (!DPUtil.empty(toolName) && !DPUtil.empty(title)) return toolName + " / " + title;
            if (!DPUtil.empty(title)) return title;
            String method = text(item, "method");
            if (!DPUtil.empty(method)) return method;
        }
        String name = text(item, "displayName");
        if (DPUtil.empty(name)) {
            String kind = kindOf(item);
            if ("knowledge".equals(kind)) name = text(item, "knowledgeName");
            else if ("theme".equals(kind)) name = text(item, "themeName");
            else if ("ontology".equals(kind)) name = text(item, "ontologyName");
            else if ("agentic".equals(kind)) name = text(item, "agenticName");
        }
        if (DPUtil.empty(name)) name = text(item, "name");
        return DPUtil.empty(name) ? functionName : name;
    }

    /**
     * 只有工具方法和编排应用的业务描述进入常驻工具说明，帮助模型选择具体目标；
     * 知识库、数据主题、知识图谱只下发固定能力模板，描述在真正调用后返回。
     * 与名称或方法标题重复时省略，避免出现「名称：名称」。
     */
    protected String detail(JsonNode item, String kind, String name) {
        if (!"method".equals(kind) && !"agentic".equals(kind)) return "";
        String value = "method".equals(kind)
                ? fullText(item, "methodDescription") : fullText(item, "description");
        if (DPUtil.empty(value) || value.equals(name)) return "";
        if (value.equals(text(item, "name"))) return "";
        if ("method".equals(kind) && value.equals(text(item, "methodTitle"))) return "";
        return value;
    }

    /** 描述补一个句末标点；已有终止标点时原样保留 */
    protected String sentence(String value) {
        String text = DPUtil.parseString(value).trim();
        if (DPUtil.empty(text)) return "";
        char last = text.charAt(text.length() - 1);
        return "。！？.!?".indexOf(last) >= 0 ? text : text + "。";
    }

    protected String text(JsonNode item, String field) {
        return text(item, field, 80);
    }

    protected String text(JsonNode item, String field, int limit) {
        String value = null == item ? "" : DPUtil.parseString(item.at("/" + field).asText(""));
        value = value.replaceAll("\\s+", " ").trim();
        return value.length() > limit ? value.substring(0, limit - 1) + "…" : value;
    }

    /** 操作者填写的描述保留全文与换行，不做字数截断。 */
    protected String fullText(JsonNode item, String field) {
        return null == item ? "" : DPUtil.parseString(item.at("/" + field).asText("")).trim();
    }

}
