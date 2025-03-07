package com.iisquare.fs.app.test.tester;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Properties;

public class ClickHouseTester {

    @Test
    public void helloTest() throws Exception {
        String url = "jdbc:ch://127.0.0.1:8123";
        Properties properties = new Properties();
        // properties.setProperty("ssl", "true");
        // properties.setProperty("sslmode", "NONE"); // NONE to trust all servers; STRICT for trusted only

        ClickHouseDataSource dataSource = new ClickHouseDataSource(url, properties);
        try (Connection connection = dataSource.getConnection("root", "admin888");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select * from system.tables limit 10")) {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columns = resultSetMetaData.getColumnCount();
            while (resultSet.next()) {
                for (int c = 1; c <= columns; c++) {
                    System.out.print(resultSetMetaData.getColumnName(c) + ":" + resultSet.getString(c) + (c < columns ? ", " : "\n"));
                }
            }
        }
    }

}
