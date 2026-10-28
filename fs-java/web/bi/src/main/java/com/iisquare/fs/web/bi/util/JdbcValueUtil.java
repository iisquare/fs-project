package com.iisquare.fs.web.bi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import io.trino.jdbc.Row;
import io.trino.jdbc.RowField;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Map;

/**
 * 将 JDBC 返回值转换为可序列化的 JSON 结构。
 * Trino 的 ARRAY 字段返回 java.sql.Array（常见实现为 TrinoArray），
 * 不能直接交给 Jackson 按 Bean 展开，否则会尝试读取其 resultSet 属性。
 */
public final class JdbcValueUtil {

    private JdbcValueUtil() {
    }

    public static JsonNode value(ResultSet resultSet, int index) throws SQLException {
        return value(resultSet.getObject(index));
    }

    public static JsonNode value(Object value) throws SQLException {
        if (null == value) return DPUtil.toJSON(null);
        if (value instanceof JsonNode node) return node;
        if (value instanceof java.sql.Array array) return value(array.getArray());
        if (value instanceof Row row) {
            ObjectNode node = DPUtil.objectNode();
            for (RowField field : row.getFields()) {
                String name = field.getName().orElse("field_" + (field.getOrdinal() + 1));
                node.set(name, value(field.getValue()));
            }
            return node;
        }
        if (value instanceof Struct struct) return value(struct.getAttributes());
        if (value instanceof Map<?, ?> map) {
            ObjectNode node = DPUtil.objectNode();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                node.set(String.valueOf(entry.getKey()), value(entry.getValue()));
            }
            return node;
        }
        if (value instanceof Iterable<?> iterable) {
            ArrayNode node = DPUtil.arrayNode();
            for (Object item : iterable) {
                node.add(value(item));
            }
            return node;
        }
        if (value.getClass().isArray()) {
            if (value instanceof byte[] bytes) return DPUtil.toJSON(bytes);
            ArrayNode node = DPUtil.arrayNode();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                node.add(value(Array.get(value, index)));
            }
            return node;
        }
        return DPUtil.toJSON(value);
    }
}
