package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.source.AbstractMongoSource;
import org.apache.spark.sql.SparkSession;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @see(https://docs.mongodb.com/spark-connector/current/configuration/)
 */
public class MongoSourceNode extends AbstractMongoSource {

    public static final String INPUT_PREFIX = "spark.mongodb.read.";
    Map<String, String> config;

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://";
        String username = options.at("/username").asText();
        if (!DPUtil.empty(username)) {
            uri += username + ":" + options.at("/password").asText() + "@";
        }
        uri += options.at("/hosts").asText() + "/";
        config.put(INPUT_PREFIX + "connection.uri", uri);
        config.put(INPUT_PREFIX + "database", options.at("/database").asText());
        config.put(INPUT_PREFIX + "collection", options.at("/collection").asText());
        String pipeline = options.at("/pipeline").asText();
        if (!DPUtil.empty(pipeline)) config.put(INPUT_PREFIX + "aggregation.pipeline", pipeline);
        this.config = config;
        return true;
    }

    @Override
    public Object process() {
        SparkSession session = runner(SparkRunner.class).session();
        return session.read().format("mongodb").options(config).load();
    }
}
