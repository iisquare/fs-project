package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.sink.AbstractElasticsearchSink;
import org.apache.spark.sql.Dataset;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;
import org.elasticsearch.spark.sql.api.java.JavaEsSparkSQL;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @see(https://www.elastic.co/guide/en/elasticsearch/hadoop/current/configuration.html)
 */
public class ElasticsearchSinkNode extends AbstractElasticsearchSink {

    String resource;
    Map<String, String> config = new LinkedHashMap<>();

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        resource = options.at("/collection").asText();
        config.put("es.nodes", options.at("/servers").asText());
        config.put("es.nodes.discovery", "false"); // default true
        config.put("es.nodes.wan.only", "true"); // default false
        config.put("es.net.http.auth.user", options.at("/username").asText());
        config.put("es.net.http.auth.pass", options.at("/password").asText());
        config.put("es.index.auto.create", "yes"); // default yes
        config.put("es.write.operation", options.at("/mode").asText("index"));
        String idField = options.at("/idField").asText();
        if (!DPUtil.empty(idField)) config.put("es.mapping.id", idField); // default none
        config.put("es.batch.size.entries", options.at("/batchSize").asText()); // default 1000
        config.put("es.batch.write.refresh", "true"); // default true
        return true;
    }

    @Override
    public Object process() {
        Dataset dataset = SparkUtil.union(Dataset.class, sources);
        if ("json".equals(options.at("/format").asText())) {
            JavaEsSpark.saveJsonToEs(dataset.toJavaRDD(), resource, config);
        } else {
            JavaEsSparkSQL.saveToEs(dataset, resource, config);
        }
        return null;
    }

}
