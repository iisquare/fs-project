package com.iisquare.fs.app.flink.tester;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.junit.Test;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JDBCTester {

    public static final Map<String, Map<String, String>> configs = new LinkedHashMap<String, Map<String, String>>() {{
        put("postgres", new LinkedHashMap<String, String>() {{
            put("url", "jdbc:postgresql://127.0.0.1:5432/postgres");
            put("username", "postgres");
            put("password", "admin888");
        }});
        put("mysql", new LinkedHashMap<String, String>() {{
            put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
            put("username", "root");
            put("password", "admin888");
        }});
    }};

    public Connection connection(String db) throws SQLException {
        Map<String, String> config = configs.get(db);
        return DriverManager.getConnection(config.get("url"), config.get("username"), config.get("password"));
    }

    @Test
    public void caseTest() throws Exception {
        int total = 2000000;
        for (Map.Entry<String, Map<String, String>> entry : configs.entrySet()) {
            System.out.println("write to " + entry);
            Connection connection = connection(entry.getKey());
            Date date = new Date(System.currentTimeMillis());
            for (int index = 0; index <= total; index++) {
                int offset = DPUtil.random(0, 10);
                date.setTime(date.getTime() + offset);
                String sql = "insert into t_time(a, xn, xd, xs, nn, nd, ns) values (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(sql);
                Timestamp timestamp = new Timestamp(date.getTime());
                String time = DPUtil.millis2dateTime(date.getTime(), "yyyy-MM-dd HH:mm:ss.SSS");
                int i = 0;
                statement.setString(++i, String.valueOf(index));
                statement.setLong(++i, date.getTime());
                statement.setTimestamp(++i, timestamp);
                statement.setString(++i, time);
                statement.setLong(++i, date.getTime());
                statement.setTimestamp(++i, timestamp);
                statement.setString(++i, time);
                int result = statement.executeUpdate();
                FileUtil.close(statement);
                if (index % 2000 == 0) {
                    System.out.println(String.format("index-%d, result-%d, date-%d time-%s", index, result, date.getTime(), time));
                }
            }
            FileUtil.close(connection);
        }
    }

}
