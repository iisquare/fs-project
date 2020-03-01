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

public class FileSourceNode extends Node {
    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        return config;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                String filepath = config.get("filepath").asText();
                String charset = config.get("charset").asText();
                String field = config.get("field").asText();
                return environment().readTextFile(filepath, charset).map(new MapFunction<String, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> map(String record) throws Exception {
                        return DPUtil.buildMap(String.class, Object.class, field, record);
                    }
                });
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                String filepath = config.get("filepath").asText();
                String charset = config.get("charset").asText();
                String field = config.get("field").asText();
                return environment().readTextFile(filepath, charset).map(new MapFunction<String, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> map(String record) throws Exception {
                        return DPUtil.buildMap(String.class, Object.class, field, record);
                    }
                });
            }
        };
    }
}
