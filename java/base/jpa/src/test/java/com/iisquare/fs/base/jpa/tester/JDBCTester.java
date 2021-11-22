package com.iisquare.fs.base.jpa.tester;

import org.junit.Test;

import java.sql.*;

public class JDBCTester {

    @Test
    public void schemaTest() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/fs_project?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Connection connection = DriverManager.getConnection(url, "root", "admin888");
        String sql = "select count(*) as ct, id from fs_member_user where id = 0 limit 3";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        ResultSetMetaData meta = rs.getMetaData();
        for (int index = 1; index <= meta.getColumnCount(); index++) {
            System.out.println("index:" + index);
            System.out.println("getColumnName:" + meta.getColumnName(index));
            System.out.println("getColumnLabel:" + meta.getColumnLabel(index));
            System.out.println("getColumnType:" + meta.getColumnType(index));
            System.out.println("getColumnTypeName:" + meta.getColumnTypeName(index));
            System.out.println("getColumnDisplaySize:" + meta.getColumnDisplaySize(index));
            System.out.println("getColumnClassName:" + meta.getColumnClassName(index));
        }
        statement.close();
        rs.close();
        connection.close();
    }

}
