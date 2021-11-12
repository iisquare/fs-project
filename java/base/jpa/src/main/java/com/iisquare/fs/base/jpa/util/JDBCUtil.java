package com.iisquare.fs.base.jpa.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCUtil {

    public static ObjectNode tables(Connection connection) throws SQLException {
        return tables(connection, "%");
    }

    public static ObjectNode tables(Connection connection, String pattern) throws SQLException {
        ObjectNode tables = DPUtil.objectNode();
        ResultSet rs = connection.getMetaData().getTables(
                null, null, pattern, new String[]{"TABLE"});
        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");
            ObjectNode item = tables.putObject(table);
            item.put("name", table);
            item.replace("columns", columns(connection, table));
        }
        JdbcUtils.closeResultSet(rs);
        return tables;
    }

    public static ObjectNode columns(Connection connection, String table) throws SQLException {
        ObjectNode columns = DPUtil.objectNode();
        ResultSet rs = connection.getMetaData().getColumns(null, null, table, "%");
        while (rs.next()) {
            String column = rs.getString("COLUMN_NAME");
            ObjectNode item = columns.putObject(column);
            item.put("name", column);
            item.put("type", rs.getString("TYPE_NAME"));
            item.put("remark", rs.getString("REMARKS"));
        }
        JdbcUtils.closeResultSet(rs);
        return columns;
    }

}
