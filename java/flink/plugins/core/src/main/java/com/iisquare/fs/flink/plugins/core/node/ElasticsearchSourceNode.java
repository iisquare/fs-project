package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.plugins.core.input.ElasticsearchInput;
import com.iisquare.fs.flink.plugins.core.source.ElasticsearchSource;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Map;

public class ElasticsearchSourceNode extends Node {
    @Override
    public JsonNode run() throws Exception {
        return DPUtil.parseJSON(this.config);
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                String cluster = config.get("cluster").asText();
                String servers = config.get("servers").asText();
                String collectionIndex = config.get("collectionIndex").asText();
                String collectionType = config.get("collectionType").asText();
                return environment()
                        .addSource(new ElasticsearchSource(cluster, servers, collectionIndex, collectionType))
                        .name(current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                String cluster = config.get("cluster").asText();
                String servers = config.get("servers").asText();
                String collectionIndex = config.get("collectionIndex").asText();
                String collectionType = config.get("collectionType").asText();
                String query = config.get("query").asText();
                return environment()
                        .createInput(new ElasticsearchInput(cluster, servers, collectionIndex, collectionType, query))
                        .name(current.getClass().getSimpleName());
            }
        };
    }
}
