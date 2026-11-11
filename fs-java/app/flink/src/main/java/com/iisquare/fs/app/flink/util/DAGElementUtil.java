package com.iisquare.fs.app.flink.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.util.Map;

/**
 * Flink DAG 通用元素处理工具，统一 Row、JSON 与字符串字段的读取和转换。
 */
public final class DAGElementUtil {

    private DAGElementUtil() {
    }

    public static Object fieldValue(Object element, RowTypeInfo rowType, String field) {
        if (element instanceof Row row) {
            if (null == rowType) throw new IllegalArgumentException("Row处理需要上游提供RowTypeInfo");
            int index = rowType.getFieldIndex(field);
            if (index < 0) throw new IllegalArgumentException("上游Row不存在字段：" + field);
            return row.getField(index);
        }
        if (element instanceof JsonNode json) return jsonValue(json, field);
        if (element instanceof Map<?, ?> map) return mapValue(map, field);
        if (element instanceof String text) {
            JsonNode json = DPUtil.parseJSON(text);
            if (null != json) return jsonValue(json, field);
        }
        JsonNode json = DPUtil.toJSON(element);
        return null == json ? null : jsonValue(json, field);
    }

    public static JsonNode jsonValue(JsonNode json, String field) {
        String path = field.trim()
                .replaceFirst("^\\$\\.?", "")
                .replace("[", ".")
                .replace("]", "")
                .replaceFirst("^\\.", "");
        return DPUtil.value(json, path);
    }

    public static JsonNode toJson(Object value, RowTypeInfo rowType) {
        if (value instanceof JsonNode json) return json;
        if (value instanceof Row row) {
            if (null == rowType) throw new IllegalArgumentException("Row序列化需要上游提供RowTypeInfo");
            String[] names = rowType.getFieldNames();
            ObjectNode json = DPUtil.objectNode();
            for (int index = 0; index < names.length; index++) {
                json.set(names[index], safeJson(row.getField(index)));
            }
            return json;
        }
        if (value instanceof String text) {
            JsonNode json = DPUtil.parseJSON(text);
            return null == json ? TextNode.valueOf(text) : json;
        }
        try {
            return safeJson(value);
        } catch (Exception e) {
            return TextNode.valueOf(String.valueOf(value));
        }
    }

    public static JsonNode safeJson(Object value) {
        JsonNode json = DPUtil.toJSON(value);
        return null == json ? NullNode.instance : json;
    }

    private static Object mapValue(Map<?, ?> map, String field) {
        Object current = map;
        for (String part : field.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) return null;
            current = currentMap.get(part);
            if (null == current) return null;
        }
        return current;
    }
}
