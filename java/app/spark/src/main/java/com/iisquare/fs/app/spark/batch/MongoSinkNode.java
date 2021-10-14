package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.sink.AbstractMongoSink;
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.config.WriteConfig;
import org.apache.spark.sql.Dataset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @see(https://docs.mongodb.com/spark-connector/current/configuration/)
 */
public class MongoSinkNode extends AbstractMongoSink {

    public static final String OUTPUT_PREFIX = "spark.mongodb.output.";

    @Override
    public Object process() {
        Dataset dataset = SparkUtil.union(Dataset.class, sources);
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://";
        String username = options.at("/username").asText();
        if (!DPUtil.empty(username)) {
            uri += username + ":" + options.at("/password").asText() + "@";
        }
        uri += options.at("/hosts").asText() + "/";
        config.put(OUTPUT_PREFIX + "uri", uri);
        config.put(OUTPUT_PREFIX + "database", options.at("/database").asText());
        config.put(OUTPUT_PREFIX + "collection", options.at("/collection").asText());
        int batchSize = options.at("/batchSize").asInt();
        if (batchSize > 0) config.put(OUTPUT_PREFIX + "maxBatchSize", String.valueOf(batchSize));
        config.put(OUTPUT_PREFIX + "replaceDocument", options.at("/replaceDocument").asText("true"));
        config.put(OUTPUT_PREFIX + "forceInsert", options.at("/forceInsert").asText("false"));
        MongoSpark.save(dataset, WriteConfig.create(config));
        return null;
    }
}
