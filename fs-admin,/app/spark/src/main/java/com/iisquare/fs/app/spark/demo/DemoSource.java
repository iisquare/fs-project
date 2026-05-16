package com.iisquare.fs.app.spark.demo;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class DemoSource {

    public static Dataset<Row> mysql(SparkSession session, String sql, int fetchSize) {
        return session.read().format("jdbc").options(DemoConfig.mysqlSource(sql, fetchSize)).load();
    }

}
