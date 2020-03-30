package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Map;

public class JsonTransformNode extends Node {
    String field;
    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);;
        field = config.get("field").asText();
        return config;
    }

    private MapFunction map(String field) {
        return new MapFunction<Map<String, Object>, Map<String, Object>>() {
            @Override
            public Map<String, Object> map(Map<String, Object> record) throws Exception {
                return DPUtil.convertJSON(DPUtil.parseJSON(DPUtil.parseString(record.get(field))), Map.class);
            }
        };
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                return current.getSource().iterator().next().getStream().result().map(map(field));
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                return current.getSource().iterator().next().getBatch().result().map(map(field));
            }
        };
    }
}
