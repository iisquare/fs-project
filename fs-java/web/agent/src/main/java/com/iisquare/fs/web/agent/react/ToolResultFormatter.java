package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 工具结果的两级输出格式。
 *
 * <p>full 保持工具原有返回值，供审计、调试和兼容调用；brief 只保留模型完成任务所需的
 * 字段，并把重复的对象字段名收敛成“列定义 + 行数组”。该参数由调用方传入，不进入
 * function schema，避免模型在每次业务调用时自行决定输出协议。</p>
 */
public final class ToolResultFormatter {

    public static final String FORMAT_FULL = "full";
    public static final String FORMAT_BRIEF = "brief";

    private ToolResultFormatter() {
    }

    public static boolean brief(String format) {
        return FORMAT_BRIEF.equalsIgnoreCase(DPUtil.trim(DPUtil.parseString(format)));
    }

    /**
     * 按工具类型生成模型视图。非 brief 原样返回，保证旧调用方的行为不变。
     */
    public static Object format(String kind, Object value, String format) {
        if (!brief(format) || null == value) return value;
        JsonNode source = DPUtil.toJSON(value);
        if (null == source || source.isNull() || source.isMissingNode() || source.isValueNode()) {
            return value;
        }
        String type = DPUtil.empty(kind) ? "method" : kind.trim().toLowerCase(Locale.ROOT);
        switch (type) {
            case "method":
                return method(source);
            case "knowledge":
                return knowledge(source);
            case "theme":
                return theme(source);
            case "ontology":
                return ontology(source);
            case "agentic":
                return agentic(source);
            default:
                return generic(source);
        }
    }

    /**
     * 工具方法：去掉请求回显和响应头，只保留执行结果。外部服务的 JSON 响应若是字符串，
     * 解析后按通用规则压缩；非 JSON 响应保持原文。业务字段本身不裁剪。
     */
    protected static JsonNode method(JsonNode source) {
        if (!source.isObject()) return generic(source);
        // 只有工具方法执行器提供的“request + response”外层才是回显结构；
        // 业务数据自己恰好带 success/error 字段时仍按普通业务结果处理。
        if (!source.has("success") || !source.has("request") || !source.has("response")) {
            return generic(source);
        }
        ObjectNode result = DPUtil.objectNode();
        copyValue(result, source, "success");
        copyValue(result, source, "status");
        copyValue(result, source, "error");
        JsonNode response = source.get("response");
        if (null != response) {
            if (response.isTextual()) {
                String text = response.asText("");
                String trimmed = text.trim();
                JsonNode parsed = trimmed.startsWith("{") || trimmed.startsWith("[")
                        ? DPUtil.parseJSON(trimmed) : null;
                if (null != parsed && !parsed.isMissingNode()) {
                    result.set("response", generic(parsed));
                } else if (!DPUtil.empty(text)) {
                    result.put("response", text);
                }
            } else {
                result.set("response", generic(response));
            }
        }
        return result.isEmpty() ? generic(source) : result;
    }

    /**
     * 未识别的工具返回结构保留全部业务字段，仅对常见的对象数组做列式压缩，
     * 避免为未知业务强加一套裁剪规则。
     */
    protected static JsonNode generic(JsonNode source) {
        if (source.isArray()) return compactArray(source);
        if (!source.isObject()) return source.deepCopy();
        ObjectNode copy = ((ObjectNode) source).deepCopy();
        for (String field : new String[]{"rows", "records", "items", "list", "data", "content"}) {
            JsonNode child = copy.get(field);
            if (null == child) continue;
            if (child.isArray() && isObjectArray(child)) {
                copy.set(field, compactArray(child));
                continue;
            }
            if (!child.isObject()) continue;
            JsonNode rows = child.get("rows");
            if (null == rows || !rows.isArray() || !isObjectArray(rows)) continue;
            ObjectNode nested = ((ObjectNode) child).deepCopy();
            JsonNode table = compactArray(rows);
            nested.set("rows", table.get("rows"));
            if (!nested.has("columns")) nested.set("columns", table.get("columns"));
            copy.set(field, nested);
        }
        return copy;
    }

    /**
     * 知识库只回填一份命中内容。原始结果里的 result 与拼接后的 text 是同一批正文，
     * 同时返回会让长文档在上下文中出现两次。
     */
    protected static JsonNode knowledge(JsonNode source) {
        if (!source.isObject()) return generic(source);
        JsonNode rows = source.get("result");
        if (null == rows || !rows.isArray()) return generic(source);
        ObjectNode result = DPUtil.objectNode();
        copyText(result, source, "knowledgeName");
        copyValue(result, source, "documents");
        Map<String, String> documentNames = documentNames(source.get("documents"));
        ArrayNode hits = result.putArray("hits");
        for (JsonNode row : rows) {
            if (!row.isObject()) continue;
            ObjectNode hit = hits.addObject();
            String documentId = row.at("/documentId").asText("");
            if (!DPUtil.empty(documentId)) copyValue(hit, row, "documentId");
            copyValue(hit, row, "segmentId");
            String documentName = row.at("/documentName").asText("");
            if (DPUtil.empty(documentName)) documentName = documentNames.getOrDefault(documentId, "");
            if (!DPUtil.empty(documentName)) hit.put("documentName", documentName);
            JsonNode content = row.get("content");
            if (null == content || content.isNull()) content = row.get("text");
            if (null != content) hit.set("content", content.deepCopy());
            JsonNode score = row.get("score");
            if (null == score || score.isNull()) score = row.get("_score");
            if (null != score && !score.isNull()) hit.set("score", score.deepCopy());
            copyValue(hit, row, "metadata");
        }
        return result;
    }

    /**
     * 数据主题：字典保留主题、数据集描述、完整字段元数据和关联关系；查询结果的
     * columns 只放字段名，columnMeta 保留标题、类型、注释等必要信息。
     */
    protected static JsonNode theme(JsonNode source) {
        if (!source.isObject()) return generic(source);
        JsonNode query = source.get("data");
        if (null != query && isQueryResult(query)) return themeQuery(source, query);
        return themeDictionary(source);
    }

    protected static JsonNode themeQuery(JsonNode source, JsonNode query) {
        ObjectNode result = DPUtil.objectNode();
        copyText(result, source, "themeName");
        copyText(result, source, "sql");
        copyText(result, source, "description");
        if (!result.has("description")) copyText(result, query, "description");
        copyValue(result, source, "datasets");
        copyValue(result, source, "relations");
        JsonNode columns = query.get("columns");
        JsonNode rows = query.get("rows");
        ObjectNode table = queryTable(columns, rows);
        result.set("columns", table.get("columns"));
        JsonNode columnMeta = table.get("columnMeta");
        if (null != columnMeta && !columnMeta.isEmpty()) result.set("columnMeta", columnMeta);
        result.set("rows", table.get("rows"));
        copyValue(result, query, "truncated");
        copyValue(result, query, "total");
        return result;
    }

    protected static JsonNode themeDictionary(JsonNode source) {
        ObjectNode result = DPUtil.objectNode();
        copyText(result, source, "themeName");
        copyText(result, source, "description");
        JsonNode datasets = source.get("datasets");
        if (null != datasets && datasets.isArray()) {
            ArrayNode rows = result.putArray("datasets");
            for (JsonNode dataset : datasets) {
                if (!dataset.isObject()) continue;
                ObjectNode item = rows.addObject();
                // relations 通过 sourceDatasetId / targetDatasetId 引用数据集，必须保留主键。
                copyValue(item, dataset, "id");
                copyText(item, dataset, "name");
                copyText(item, dataset, "description");
                JsonNode columns = dataset.get("columns");
                if (null != columns && columns.isArray()) {
                    item.set("columns", dictionaryColumns(columns));
                }
            }
        }
        copyValue(result, source, "relations");
        return result;
    }

    /**
     * 知识图谱：实体检索使用列式行；路径推理保留可读路径文本，
     * 不再同时回填原始 paths / nodes / relationships 三份图结构。
     */
    protected static JsonNode ontology(JsonNode source) {
        if (!source.isObject()) return generic(source);
        boolean path = "path".equals(source.at("/action").asText(""))
                || source.has("paths") || source.has("text");
        ObjectNode result = DPUtil.objectNode();
        copyText(result, source, "description");
        if (path) {
            copyText(result, source, "action");
            copyValue(result, source, "truncated");
            copyValue(result, source, "from");
            copyValue(result, source, "to");
            copyValue(result, source, "text");
            return result;
        }
        copyText(result, source, "entity");
        copyText(result, source, "keyword");
        copyValue(result, source, "total");
        JsonNode rows = source.get("rows");
        if (null == rows || !rows.isArray()) return result;
        JsonNode table = compactArray(rows);
        if (table.isObject()) {
            result.set("columns", table.get("columns"));
            result.set("rows", table.get("rows"));
        } else {
            result.set("rows", table);
        }
        return result;
    }

    /**
     * 编排工具本身返回的就是子任务摘要。成功时只留 answer，失败时补上状态与原因。
     */
    protected static JsonNode agentic(JsonNode source) {
        if (!source.isObject()) return source.deepCopy();
        ObjectNode result = DPUtil.objectNode();
        copyValue(result, source, "answer");
        String error = source.at("/error").asText("");
        if (!DPUtil.empty(error)) {
            copyValue(result, source, "status");
            result.put("error", error);
        }
        return result.isEmpty() ? ((ObjectNode) source).deepCopy() : result;
    }

    /**
     * 对象数组 → { columns, rows }。字段名只出现一次，值继续使用原 JSON 类型。
     * 非对象数组或空数组原样保留，避免破坏路径、文本列表等业务语义。
     */
    protected static JsonNode compactArray(JsonNode rows) {
        if (null == rows || !rows.isArray() || rows.isEmpty()) {
            return null == rows ? DPUtil.arrayNode() : rows.deepCopy();
        }
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode row : rows) {
            if (!row.isObject()) return rows.deepCopy();
            Iterator<String> iterator = row.fieldNames();
            while (iterator.hasNext()) names.add(iterator.next());
        }
        if (names.isEmpty()) return rows.deepCopy();
        ArrayNode columns = DPUtil.arrayNode();
        for (String name : names) columns.add(name);
        ArrayNode values = DPUtil.arrayNode();
        for (JsonNode row : rows) {
            ArrayNode line = values.addArray();
            for (String name : names) {
                JsonNode value = row.get(name);
                if (null == value || value.isNull()) line.addNull();
                else line.add(value.deepCopy());
            }
        }
        ObjectNode result = DPUtil.objectNode();
        result.set("columns", columns);
        result.set("rows", values);
        return result;
    }

    protected static ObjectNode queryTable(JsonNode columns, JsonNode rows) {
        if (null == columns || !columns.isArray() || columns.isEmpty()
                || null == rows || !rows.isArray()) {
            JsonNode table = compactArray(rows);
            if (table != null && table.isObject()) return (ObjectNode) table;
            ObjectNode empty = DPUtil.objectNode();
            empty.set("columns", DPUtil.arrayNode());
            empty.set("rows", DPUtil.arrayNode());
            return empty;
        }
        ArrayNode briefColumns = DPUtil.arrayNode();
        ArrayNode names = DPUtil.arrayNode();
        for (JsonNode column : columns) {
            String name = column.at("/name").asText("");
            if (DPUtil.empty(name)) name = column.at("/label").asText("");
            names.add(name);
            ObjectNode item = briefColumns.addObject();
            if (!DPUtil.empty(name)) item.put("name", name);
            String label = column.at("/label").asText("");
            if (!DPUtil.empty(label)) item.put("label", label);
            String type = column.at("/type").asText("");
            if (!DPUtil.empty(type)) item.put("type", type);
            copyText(item, column, "title");
            copyText(item, column, "comment");
            copyText(item, column, "description");
            copyValue(item, column, "nullable");
            copyValue(item, column, "precision");
            copyValue(item, column, "scale");
        }
        ArrayNode values = DPUtil.arrayNode();
        for (JsonNode row : rows) {
            if (!row.isObject()) {
                values.add(row.deepCopy());
                continue;
            }
            ArrayNode line = values.addArray();
            for (JsonNode name : names) {
                JsonNode value = row.get(name.asText(""));
                if (null == value || value.isNull()) line.addNull();
                else line.add(value.deepCopy());
            }
        }
        ObjectNode result = DPUtil.objectNode();
        result.set("columns", names);
        result.set("columnMeta", briefColumns);
        result.set("rows", values);
        return result;
    }

    protected static ArrayNode dictionaryColumns(JsonNode columns) {
        ArrayNode result = DPUtil.arrayNode();
        for (JsonNode column : columns) {
            if (!column.isObject()) continue;
            // 字典用于编写查询：字段名、标题、类型、注释等都属于必要信息，原样保留。
            result.add(column.deepCopy());
        }
        return result;
    }

    protected static Map<String, String> documentNames(JsonNode documents) {
        Map<String, String> result = new LinkedHashMap<>();
        if (null == documents || !documents.isArray()) return result;
        for (JsonNode document : documents) {
            String id = document.at("/id").asText("");
            String name = document.at("/name").asText("");
            if (!DPUtil.empty(id) && !DPUtil.empty(name)) result.put(id, name);
        }
        return result;
    }

    protected static boolean isQueryResult(JsonNode value) {
        return null != value && value.isObject()
                && value.get("columns") != null && value.get("columns").isArray()
                && value.get("rows") != null && value.get("rows").isArray();
    }

    protected static boolean isObjectArray(JsonNode value) {
        if (null == value || !value.isArray() || value.isEmpty()) return false;
        for (JsonNode item : value) {
            if (!item.isObject()) return false;
        }
        return true;
    }

    protected static void copyText(ObjectNode target, JsonNode source, String field) {
        String value = source.at("/" + field).asText("");
        if (!DPUtil.empty(value)) target.put(field, value);
    }

    protected static void copyValue(ObjectNode target, JsonNode source, String field) {
        JsonNode value = source.get(field);
        if (null != value && !value.isMissingNode() && !value.isNull()) {
            target.set(field, value.deepCopy());
        }
    }
}
