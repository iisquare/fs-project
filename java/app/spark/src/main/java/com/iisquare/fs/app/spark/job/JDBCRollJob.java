package com.iisquare.fs.app.spark.job;

import com.iisquare.fs.app.spark.util.ConfigUtil;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCRollRelationProvider;
import com.mysql.cj.jdbc.Driver;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JDBCRollJob {
    public static void main(String[] args) {
        SparkSession session = SparkSession.builder().appName("jdbc-roll").master("local").getOrCreate();
        Map<String, String> options = new LinkedHashMap<>();
        options.put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
        options.put("driver", Driver.class.getName());
        options.put("user", "root");
        options.put("password", "admin888");
        options.put("roll", "name, id");
        options.put("batchSize", "2");
        options.put("query", "select * from fs_project.fs_member_user");
        Dataset<Row> dataset = session.read().format(JDBCRollRelationProvider.class.getName()).options(options).load();
        dataset.explain();
        dataset.printSchema();
        dataset.show(5);
        List<Row> rows = dataset.limit(3).collectAsList();
        System.out.println(rows);
        Map<String, String> smap = ConfigUtil.mysqlSink("fs_member_user", 3);
        smap.put("roll", "id");
        dataset.write().mode(SaveMode.Append).format(JDBCRollRelationProvider.class.getName()).options(smap).save();
        session.close();
    }
}
