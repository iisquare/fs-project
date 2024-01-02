package com.iisquare.fs.app.spark.util;

import com.clickhouse.jdbc.ClickHouseDriver;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigUtil {

    public static SparkConf spark() {
        SparkConf config = new SparkConf();
        if (DPUtil.empty(System.getenv("spark.master"))) config.setMaster("local[*]");
        return config;
    }

    public static Map<String, String> neo4j() {
        return new LinkedHashMap<String, String>() {{
            put("uri", "neo4j://127.0.0.1:7687");
            put("username", "neo4j");
            put("password", "admin888");
        }};
    }

    public static Map<String, String> mysql() {
        return new LinkedHashMap<String, String>() {{
            put("driver", com.mysql.cj.jdbc.Driver.class.getName());
            put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
            put("user", "root");
            put("password", "admin888");
        }};
    }

    public static Map<String, String> clickhouse() {
        return new LinkedHashMap<String, String>() {{
            put("driver", ClickHouseDriver.class.getName());
            put("url", "jdbc:ch://127.0.0.1:8123");
            put("user", "root");
            put("password", "admin888");
        }};
    }

    public static Map<String, String> elasticsearch() {
        return new LinkedHashMap<String, String>() {{
            put("es.nodes", "127.0.0.1:9200");
            put("es.nodes.discovery", "false");
            put("es.nodes.wan.only", "true");
            put("es.index.auto.create", "yes");
            put("es.write.operation", "index");
        }};
    }

    public static Map<String, String> mysqlSource(String sql, int fetchSize) {
        Map<String, String> mysql = mysql();
        mysql.put("fetchsize", String.valueOf(fetchSize));
        mysql.put("query", sql);
        return mysql;
    }

    public static Map<String, String> mysqlSink(String table, int batchSize) {
        Map<String, String> mysql = mysql();
        mysql.put("isolationLevel", "NONE");
        mysql.put("truncate", "true");
        mysql.put("dbtable", table);
        mysql.put("batchsize", String.valueOf(batchSize));
        return mysql;
    }

    public static Map<String, String> clickhouseSink(String table, int batchSize) {
        Map<String, String> clickhouse = clickhouse();
        clickhouse.put("isolationLevel", "NONE");
        clickhouse.put("truncate", "true");
        clickhouse.put("dbtable", table);
        clickhouse.put("batchsize", String.valueOf(batchSize));
        return clickhouse;
    }

}
