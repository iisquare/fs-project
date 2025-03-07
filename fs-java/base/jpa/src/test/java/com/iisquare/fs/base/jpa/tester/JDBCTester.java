package com.iisquare.fs.base.jpa.tester;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import org.junit.Test;

import java.sql.*;

public class JDBCTester {

    @Test
    public void idTest() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Connection connection = DriverManager.getConnection(url, "root", "admin888");
        String sql = String.format("insert into t_memory (name) values ('%s')", DPUtil.random(6));
        // 通过传入第二个参数,就会产生主键返回给我们
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        System.out.println("Effect:" + statement.executeUpdate());;
        // 返回的结果集中包含主键,注意：主键还可以是UUID,
        // 复合主键等,所以这里不是直接返回一个整型
        ResultSet rs = statement.getGeneratedKeys();
        if(rs.next()) {
            System.out.println("Key:" + rs.getObject(1));;
        }
        statement.close();
        rs.close();
        connection.close();
    }

    @Test
    public void metaTest() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/fs_project?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Connection connection = DriverManager.getConnection(url, "root", "admin888");
        ObjectNode tables = JDBCUtil.tables(connection);
        System.out.println(tables);
        connection.close();
    }

    @Test
    public void batchTest() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Connection connection = DriverManager.getConnection(url, "root", "admin888");
        String sql = "select * from t_memory";
        PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setFetchSize(Integer.MIN_VALUE);
        // ((com.mysql.jdbc.Statement) statement).enableStreamingResults();
        statement.setFetchDirection(ResultSet.FETCH_REVERSE);
        ResultSet rs = statement.executeQuery();
        int index = 0;
        while (rs.next()) {
            if (index++ > 100) break;
            System.out.println(rs.getObject(1));
        }
        FileUtil.close(rs, statement, connection);
    }

}
