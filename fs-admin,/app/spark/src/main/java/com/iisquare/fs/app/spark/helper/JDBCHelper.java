package com.iisquare.fs.app.spark.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.tool.SQLBuilder;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.SQLUtil;
import org.apache.commons.collections.IteratorUtils;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class JDBCHelper implements Closeable {

    private final Connection connection;

    public JDBCHelper(Map<String, String> config) throws Exception {
        Class.forName(config.get("driver"));
        connection = DriverManager.getConnection(config.get("url"), config.get("user"), config.get("password"));
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(connection);
    }

    public Connection connection() {
        return connection;
    }

    public int batch(String table, ArrayNode arr, int batchSize, String... updateFields) throws SQLException {
        return batch(table, arr, batchSize, updateFields.length == 0, updateFields);
    }

    public int batch(String table, ArrayNode arr, int batchSize, boolean updateFull, String... updateFields) throws SQLException {
        int count = 0;
        int size = arr.size();
        ArrayNode data = DPUtil.arrayNode();
        for (int i = 0; i < size; i++) {
            data.add(arr.get(i));
            if (data.size() >= batchSize || i + 1 >= size) {
                count++;
                batch(table, data, updateFull, updateFields);
                data = DPUtil.arrayNode();
            }
        }
        return count;
    }

    public int batch(String table, ArrayNode data, String... updateFields) throws SQLException {
        return batch(table, data, updateFields.length == 0, updateFields);
    }

    public int batch(String table, ArrayNode data, boolean updateFull, String... updateFields) throws SQLException {
        Set<String> keys = new LinkedHashSet<>(IteratorUtils.toList(data.get(0).fieldNames()));
        List<String> values = new ArrayList<>();
        for (JsonNode node : data) {
            List<Object> list = new ArrayList<>();
            for (String key : keys) {
                Object value = DPUtil.toJSON(node.get(key), Object.class);
                if(null == value) {
                    value = "NULL";
                } else {
                    value = "'" + SQLUtil.escape(value.toString()) + "'";
                }
                list.add(value);
            }
            values.add("(" + DPUtil.implode(", ", DPUtil.toArray(Object.class, list)) + ")");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table);
        sb.append(" (").append(DPUtil.implode(", ", DPUtil.toArray(String.class, keys))).append(")");
        sb.append(" VALUES ").append(DPUtil.implode(", ", DPUtil.toArray(String.class, values)));
        sb.append(updateFull ? SQLBuilder.duplicateUpdate(keys) : SQLBuilder.duplicateUpdate(updateFields));
        return update(sb.toString());
    }

    public ArrayNode query(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        ArrayNode result = result(rs);
        FileUtil.close(rs, statement);
        return result;
    }

    public long number(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        long result = -1;
        if (rs.next()) {
            result = rs.getLong(1);
        }
        FileUtil.close(rs, statement);
        return result;
    }

    public int update(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        int result = statement.executeUpdate(sql);
        FileUtil.close(statement);
        return result;
    }

    public static ArrayNode result(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        ArrayNode result = DPUtil.arrayNode();
        while (rs.next()) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
                item.put(meta.getColumnName(i), rs.getObject(i));
            }
            result.add(DPUtil.toJSON(item));
        }
        return result;
    }

    public static Row row(ResultSet rs, StructType schema) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        Object[] values = new Object[count];
        for (int i = 1; i <= count; i++) {
            values[i - 1] = rs.getObject(i);
        }
        return SparkUtil.row(schema, values);
    }

    public static String pkWhere(Row row, String... pks) {
        if (null == row) return "1 = 1";
        List<String> list = new ArrayList<>();
        for (int i = 0; i < pks.length; i++) {
            List<String> items = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                String text = SQLUtil.escape(DPUtil.parseString(row.getAs(pks[i])));
                items.add(String.format("%s %s '%s'", pks[j], j == i ? ">" : "=", text));
            }
            list.add(String.format("(%s)", DPUtil.implode(" AND ", items)));
        }
        return DPUtil.implode(" OR ", list);
    }

}
