package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractElasticsearchSource;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.elasticsearch.spark.sql.api.java.JavaEsSparkSQL;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Elasticsearch 批量读取。
 *
 * query 使用 Elasticsearch Query DSL JSON；调试和节点显式 limit 通过 scroll limit
 * 限制实际拉取文档数，同时仍对整个 Dataset 应用限流。
 */
public class ElasticsearchSourceNode extends AbstractElasticsearchSource<Dataset<Row>> {

    private String resource;
    private Map<String, String> config;
    private int effectiveLimit;

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        resource = options.at("/collection").asText(options.at("/index").asText(""));
        if (DPUtil.empty(resource)) return false;
        String servers = options.at("/servers").asText("");
        if (DPUtil.empty(servers)) return false;
        int debugLimit = options.at("/debugLimit").asInt(0);
        int nodeLimit = options.at("/limit").asInt(0);
        effectiveLimit = effectiveLimit(nodeLimit, debugLimit);
        Map<String, String> conf = new LinkedHashMap<>();
        conf.put("es.nodes", servers);
        conf.put("es.nodes.discovery", "false");
        conf.put("es.nodes.wan.only", "true");
        conf.put("es.net.http.auth.user", options.at("/username").asText(""));
        conf.put("es.net.http.auth.pass", options.at("/password").asText(""));
        String sourceFields = options.at("/sourceFields").asText("");
        if (!DPUtil.empty(sourceFields)) conf.put("es.read.source.filter", sourceFields);
        conf.put("es.read.metadata", "false");
        if (effectiveLimit > 0) {
            conf.put("es.scroll.limit", String.valueOf(effectiveLimit));
            conf.put("es.scroll.size", String.valueOf(Math.min(effectiveLimit, 1000)));
        }
        JsonNode query = options.get("query");
        if (query != null && !query.isNull()) {
            if (query.isTextual() && query.asText().trim().startsWith("{")) {
                query = DPUtil.parseJSON(query.asText());
            }
            if (query != null && query.isObject()) {
                conf.put("es.query", DPUtil.stringify(query));
            } else if (query.isTextual() && !DPUtil.empty(query.asText(""))) {
                conf.put("es.query", query.asText());
            }
        }
        this.config = conf;
        return true;
    }

    static int effectiveLimit(int nodeLimit, int debugLimit) {
        int result = 0;
        if (nodeLimit > 0) result = nodeLimit;
        if (debugLimit > 0) result = result > 0 ? Math.min(result, debugLimit) : debugLimit;
        return result;
    }

    @Override
    public Dataset<Row> process() {
        Dataset<Row> dataset = JavaEsSparkSQL.esDF(runner(SparkRunner.class).session(), resource, config);
        return effectiveLimit > 0 ? dataset.limit(effectiveLimit) : dataset;
    }
}
