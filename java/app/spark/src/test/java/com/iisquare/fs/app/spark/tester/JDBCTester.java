package com.iisquare.fs.app.spark.tester;

import com.mysql.cj.jdbc.Driver;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JDBCTester {

    @Test
    public void sql1Test() {
        SparkSession session = SparkSession.builder().appName("sql-test").master("local").getOrCreate();
        Map<String, String> options = new LinkedHashMap<>();
        options.put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
        options.put("driver", Driver.class.getName());
        options.put("user", "root");
        options.put("password", "admin888");
        options.put("query", "select * from dwd_company_info");
        options.put("pushDownLimit", "true");
        options.put("fetchsize", String.valueOf(Integer.MIN_VALUE));
//        options.put("dbtable", "dwd_company_info");
        Dataset<Row> dataset = session.read().format("jdbc").options(options).load();
        List<Row> rows = dataset.limit(3).collectAsList();
        System.out.println(rows);
        dataset.explain();
        session.close();
    }

    @Test
    public void sql2Test() {
        SparkSession session = SparkSession.builder().appName("sql-test").master("local").getOrCreate();
        String url = "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Properties properties = new Properties();
        properties.put("driver", "com.mysql.cj.jdbc.Driver");
        properties.put("user", "root");
        properties.put("password", "admin888");
        properties.put("pushDownLimit", "true");
        properties.put("fetchsize", String.valueOf(Integer.MIN_VALUE));
        Dataset<Row> dataset = session.read().jdbc(url, "dwd_company_info", properties);
        dataset = dataset.map((MapFunction<Row, Row>) value -> {
            System.out.println("M:" + value);
            return value;
        }, dataset.encoder());
        dataset = dataset.limit(10).map((MapFunction<Row, Row>) value -> {
            System.out.println("L:" + value);
            return value;
        }, dataset.encoder());
        dataset.explain(true);
        dataset.foreachPartition((ForeachPartitionFunction<Row>) t -> {
            System.out.println("ForeachPartitionFunction:");
            while (t.hasNext()) {
                Row row = t.next();
                System.out.println("F:" + row);
            }
        });
        session.close();
    }

}
