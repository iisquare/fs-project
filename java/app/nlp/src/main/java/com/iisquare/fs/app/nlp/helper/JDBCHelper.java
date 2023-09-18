package com.iisquare.fs.app.nlp.helper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public ArrayNode query(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        ArrayNode result = result(rs);
        FileUtil.close(rs, statement);
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
