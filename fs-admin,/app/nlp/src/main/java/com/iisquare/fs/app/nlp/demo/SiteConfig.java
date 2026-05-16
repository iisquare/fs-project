package com.iisquare.fs.app.nlp.demo;

import java.util.LinkedHashMap;
import java.util.Map;

public class SiteConfig {

    public static Map<String, String> mysql() {
        return new LinkedHashMap<>() {{
            put("driver", com.mysql.cj.jdbc.Driver.class.getName());
            put("url", "jdbc:mysql://127.0.0.1:3306/fs_site?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
            put("user", "root");
            put("password", "admin888");
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

}
