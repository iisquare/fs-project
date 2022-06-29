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

}
