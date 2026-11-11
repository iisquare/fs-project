package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractMongoSource;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @see(https://docs.mongodb.com/spark-connector/current/configuration/)
 */
public class MongoSourceNode extends AbstractMongoSource<Dataset<Row>> {

    public static final String INPUT_PREFIX = "spark.mongodb.read.";
    Map<String, String> config;

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        Map<String, String> config = new LinkedHashMap<>();
        String uri = options.at("/uri").asText();
        if (DPUtil.empty(uri)) {
            uri = "mongodb://";
            String username = options.at("/username").asText();
            if (!DPUtil.empty(username)) {
                uri += username + ":" + options.at("/password").asText() + "@";
            }
            uri += options.at("/hosts").asText() + "/";
        }
        config.put(INPUT_PREFIX + "connection.uri", uri);
        config.put(INPUT_PREFIX + "database", options.at("/database").asText());
        config.put(INPUT_PREFIX + "collection", options.at("/collection").asText());
        String pipeline = options.at("/pipeline").asText();
        int debugLimit = options.at("/debugLimit").asInt(0);
        if (debugLimit > 0) {
            JsonNode parsed = DPUtil.parseJSON(pipeline);
            ArrayNode stages;
            if (null == parsed || parsed.isNull()) {
                stages = DPUtil.arrayNode();
            } else if (parsed.isArray()) {
                stages = (ArrayNode) parsed;
            } else {
                return false;
            }
            ObjectNode limitStage = stages.addObject();
            limitStage.put("$limit", debugLimit);
            pipeline = DPUtil.stringify(stages);
            config.put(INPUT_PREFIX + "partitioner",
                    "com.mongodb.spark.sql.connector.read.partitioner.SinglePartitionPartitioner");
        }
        if (!DPUtil.empty(pipeline)) config.put(INPUT_PREFIX + "aggregation.pipeline", pipeline);
        this.config = config;
        return true;
    }

    @Override
    public Dataset<Row> process() {
        SparkSession session = runner(SparkRunner.class).session();
        return session.read().format("mongodb").options(config).load();
    }
}
