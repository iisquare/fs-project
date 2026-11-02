package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * function calling 函数名归一：模型侧只接收字母、数字、下划线与短横线。
 *
 * 同一份工具清单必须用相同的顺序调用 {@link #unique(JsonNode, Set)}，
 * 定义与执行才能把模型返回的名称稳定映射回原工具项。
 */
public final class ToolFunctionName {

    private static final Pattern INVALID = Pattern.compile("[^0-9a-zA-Z_-]");
    private static final Pattern REPEATED = Pattern.compile("_{2,}");
    private static final int MAX_BASE_LENGTH = 60;

    private ToolFunctionName() {
    }

    public static String unique(JsonNode item, Set<String> used) {
        String base = base(item);
        String name = base;
        int index = 2;
        while (used.contains(name)) {
            name = base + "_" + index;
            index++;
        }
        used.add(name);
        return name;
    }

    protected static String base(JsonNode item) {
        String kind = item.at("/kind").asText("method");
        if (DPUtil.empty(kind)) kind = "method";
        String source = "method".equals(kind)
                ? item.at("/method").asText("")
                : item.at("/name").asText("");
        String base = INVALID.matcher(DPUtil.parseString(source)).replaceAll("_");
        base = REPEATED.matcher(base).replaceAll("_").replaceAll("^_+|_+$", "");
        if (DPUtil.empty(base)) base = kind + "_" + id(item, kind);
        return base.length() > MAX_BASE_LENGTH ? base.substring(0, MAX_BASE_LENGTH) : base;
    }

    protected static int id(JsonNode item, String kind) {
        int id = item.at("/id").asInt(0);
        if (id > 0) return id;
        if ("method".equals(kind)) return item.at("/toolId").asInt(0);
        if ("knowledge".equals(kind)) return item.at("/knowledgeId").asInt(0);
        if ("theme".equals(kind)) return item.at("/themeId").asInt(0);
        if ("ontology".equals(kind)) return item.at("/ontologyId").asInt(0);
        if ("agentic".equals(kind)) return item.at("/agenticId").asInt(0);
        return 0;
    }
}
