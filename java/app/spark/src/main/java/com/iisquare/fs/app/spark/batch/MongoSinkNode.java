package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.sink.AbstractMongoSink;
import org.apache.spark.sql.Dataset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @see(https://docs.mongodb.com/spark-connector/current/configuration/)
 */
public class MongoSinkNode extends AbstractMongoSink {

    public static final String OUTPUT_PREFIX = "spark.mongodb.write.";
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
        config.put(OUTPUT_PREFIX + "connection.uri", uri);
        config.put(OUTPUT_PREFIX + "database", options.at("/database").asText());
        config.put(OUTPUT_PREFIX + "collection", options.at("/collection").asText());
        int batchSize = options.at("/batchSize").asInt();
        if (batchSize > 0) config.put(OUTPUT_PREFIX + "maxBatchSize", String.valueOf(batchSize));
        String idFieldList = options.at("/idFieldList").asText();
        if (!DPUtil.empty(idFieldList)) config.put(OUTPUT_PREFIX + "idFieldList", idFieldList);
        config.put(OUTPUT_PREFIX + "operationType", options.at("/operationType").asText("update"));
        this.config = config;
        return true;
    }

    @Override
    public Object process() {
        Dataset dataset = SparkUtil.union(Dataset.class, sources);
        dataset.write().format("mongodb").mode("append").options(config).save();
        return null;
    }
}
