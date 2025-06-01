package com.iisquare.fs.base.jpa.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.jdbc.support.JdbcUtils;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JDBCUtil {

    public static Connection connection(JsonNode config) throws SQLException {
        return DriverManager.getConnection(config.at("/url").asText(),
                config.at("/username").asText(), config.at("/password").asText());
    }

    public static ObjectNode tables(Connection connection) throws SQLException {
        return tables(connection, null, new String[]{"TABLE"});
    }

    public static ObjectNode tables(Connection connection, String pattern, String[] types) throws SQLException {
        ObjectNode tables = DPUtil.objectNode();
        String catalog = connection.getCatalog();
        if ("".equals(catalog)) catalog = null;
        String schema = connection.getSchema();
        if ("".equals(schema)) schema = null;
        ResultSet rs = connection.getMetaData().getTables(catalog, schema, pattern, types);
        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");
            ObjectNode item = tables.putObject(table);
            item.put("name", table);
            item.put("type", rs.getString("TABLE_TYPE"));
            item.replace("columns", columns(connection, table));
        }
        JdbcUtils.closeResultSet(rs);
        return tables;
    }

    public static List<String> pks(Connection connection, String table) throws SQLException {
        List<String> result = new ArrayList<>();
        ResultSet rs = connection.getMetaData().getPrimaryKeys(null, null, table);
        while (rs.next()) {
            result.add(rs.getString("column_name"));
        }
        return result;
    }

    public static ObjectNode columns(Connection connection, String table) throws SQLException {
        ObjectNode columns = DPUtil.objectNode();
        ResultSet rs = connection.getMetaData().getColumns(null, null, table, null);
        while (rs.next()) {
            String column = rs.getString("COLUMN_NAME");
            ObjectNode item = columns.putObject(column);
            item.put("name", column);
            item.put("type", rs.getString("TYPE_NAME"));
            item.put("jdbcType", JDBCType.valueOf(rs.getInt("DATA_TYPE")).name());
            item.put("size", rs.getInt("COLUMN_SIZE"));
            item.put("digit", rs.getInt("DECIMAL_DIGITS"));
            item.put("nullable", rs.getInt("NULLABLE") != 0);
            item.replace("defaultValue", DPUtil.toJSON(rs.getObject("COLUMN_DEF")));
            item.put("position", rs.getInt("ORDINAL_POSITION"));
            item.put("remark", rs.getString("REMARKS"));
        }
        JdbcUtils.closeResultSet(rs);
        return columns;
    }

    public static ObjectNode tables(Connection connection, String table, String sql) throws SQLException {
        ObjectNode tables = DPUtil.objectNode();
        ObjectNode item = tables.putObject(table);
        item.put("name", table);
        item.replace("columns", columns(connection, table, sql));
        return tables;
    }

    public static ObjectNode columns(Connection connection, String table, String sql) throws SQLException {
        ObjectNode columns = DPUtil.objectNode();
        if (DPUtil.empty(table)) return columns;
        if (DPUtil.empty(sql)) {
            sql = String.format("select * from %s limit 0", table);
        } else {
            sql = String.format("select * from (%s) as %s limit 0", sql, table);
        }
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        columns = columns(rs);
        JdbcUtils.closeStatement(statement);
        JdbcUtils.closeResultSet(rs);
        return columns;
    }

    public static ObjectNode columns(ResultSet rs) throws SQLException {
        ObjectNode columns = DPUtil.objectNode();
        ResultSetMetaData meta = rs.getMetaData();
        for (int index = 1; index <= meta.getColumnCount(); index++) {
            String column = meta.getColumnName(index);
            ObjectNode item = columns.putObject(column);
            item.put("name", column);
            item.put("type", meta.getColumnTypeName(index));
            item.put("jdbcType", JDBCType.valueOf(meta.getColumnType(index)).name());
        }
        return columns;
    }

    public static Object blob2object(Blob blob) throws SQLException, IOException, ClassNotFoundException {
        Object obj = null;
        if (blob != null && blob.length() != 0) {
            InputStream binaryInput = blob.getBinaryStream();

            if (null != binaryInput) {
                if (binaryInput instanceof ByteArrayInputStream
                        && ((ByteArrayInputStream) binaryInput).available() == 0 ) {
                    // do nothing
                } else {
                    ObjectInputStream in = new ObjectInputStream(binaryInput);
                    try {
                        obj = in.readObject();
                    } finally {
                        in.close();
                    }
                }
            }
        }
        return obj;
    }

    public static ByteArrayOutputStream object2byte(Object obj) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        if (null != obj) {
            ObjectOutputStream out = new ObjectOutputStream(bo);
            out.writeObject(obj);
            out.flush();
        }
        return bo;
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
