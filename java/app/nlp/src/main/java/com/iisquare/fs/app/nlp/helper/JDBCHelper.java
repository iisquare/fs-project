package com.iisquare.fs.app.nlp.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.tool.SQLBuilder;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.SQLUtil;
import org.apache.commons.collections.IteratorUtils;

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

    public int batch(String table, ArrayNode arr, int batchSize, String... uk) throws SQLException {
        int count = 0;
        int size = arr.size();
        ArrayNode data = DPUtil.arrayNode();
        for (int i = 0; i < size; i++) {
            data.add(arr.get(i));
            if (data.size() >= batchSize || i + 1 >= size) {
                count++;
                batch(table, data, uk);
                data = DPUtil.arrayNode();
            }
        }
        return count;
    }

    public int batch(String table, ArrayNode data, String... updateFields) throws SQLException {
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
        sb.append(SQLBuilder.duplicateUpdate(updateFields));
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

}
