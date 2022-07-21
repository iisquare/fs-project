package com.iisquare.fs.web.bi.service;

import com.iisquare.fs.app.spark.batch.MongoSourceNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TestService {

    @Autowired
    private SparkService sparkService;

    public Map<String, Object> mongoPushDown(Map<?, ?> arg) {
        SparkSession session = sparkService.session().newSession();
        session.read().format("mongodb").options(new LinkedHashMap<String, String>(){{
            put(MongoSourceNode.INPUT_PREFIX + "connection.uri", "mongodb://root:admin888@127.0.0.1:27017");
            put(MongoSourceNode.INPUT_PREFIX + "database", "fs_project");
            put(MongoSourceNode.INPUT_PREFIX + "collection", "fs.resource");
        }}).load().createOrReplaceTempView("resource");
        session.read().format("mongodb").options(new LinkedHashMap<String, String>(){{
            put(MongoSourceNode.INPUT_PREFIX + "connection.uri", "mongodb://root:admin888@127.0.0.1:27017");
            put(MongoSourceNode.INPUT_PREFIX + "database", "fs_project");
            put(MongoSourceNode.INPUT_PREFIX + "collection", "fs.test");
        }}).load().createOrReplaceTempView("test");
        Dataset<Row> dataset = session.sql(DPUtil.parseString(arg.get("sql")));
        dataset.show();
        return ApiUtil.result(0, null, dataset.count());
    }

    public Map<String, Object> mysqlPushDown(Map<?, ?> arg) {
        SparkSession session = sparkService.session().newSession();
        session.read().format("jdbc").options(new LinkedHashMap<String, String>(){{
            put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
            put("dbtable", "fs_member_resource");
            put("driver", "com.mysql.jdbc.Driver");
            put("user", "root");
            put("password", "admin888");
        }}).load().createOrReplaceTempView("resource");
        session.read().format("jdbc").options(new LinkedHashMap<String, String>(){{
            put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
            put("query", "select * from t_memory");
            put("driver", "com.mysql.jdbc.Driver");
            put("user", "root");
            put("password", "admin888");
        }}).load().createOrReplaceTempView("test");
        Dataset<Row> dataset = session.sql(DPUtil.parseString(arg.get("sql")));
        dataset.show();
        return ApiUtil.result(0, null, dataset.count());
    }

    public Map<String, Object> pushDown(Map<?, ?> arg) {
        SparkSession session = sparkService.session().newSession();
        session.read().format("mongodb").options(new LinkedHashMap<String, String>(){{
            put(MongoSourceNode.INPUT_PREFIX + "connection.uri", "mongodb://root:admin888@127.0.0.1:27017");
            put(MongoSourceNode.INPUT_PREFIX + "database", "fs_project");
            put(MongoSourceNode.INPUT_PREFIX + "collection", "fs.test");
        }}).load().createOrReplaceTempView("mongodb");
        session.read().format("jdbc").options(new LinkedHashMap<String, String>(){{
            put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
            put("dbtable", "t_memory");
            put("driver", "com.mysql.jdbc.Driver");
            put("user", "root");
            put("password", "admin888");
        }}).load().createOrReplaceTempView("mysql");
        Dataset<Row> dataset = session.sql(DPUtil.parseString(arg.get("sql")));
        dataset.show();
        return ApiUtil.result(0, null, dataset.count());
    }

}
