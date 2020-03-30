package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.plugins.core.output.EmptyOutput;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Arrays;
import java.util.Map;

public class ConsoleSinkNode extends Node {
    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        return config;
    }

    @Override
    public StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                String mode = config.get("mode").asText();
                for(Node item : source) {
                    switch (mode) {
                        case "map":
                            DataStream<Map<String, Object>> mapResult = item.getStream().result();
                            if(null == mapResult) continue;
                            mapResult.print();
                            break;
                        case "json":
                            DataStream<Map<String, Object>> jsonResult = item.getStream().result();
                            if(null == jsonResult) continue;
                            jsonResult.map(new MapFunction<Map<String,Object>, String>() {
                                @Override
                                public String map(Map<String, Object> value) throws Exception {
                                    return DPUtil.stringify(value);
                                }
                            }).name("Stringify").print();
                            break;
                        default:
                            DataStream<Map<String, Object>> result = item.getStream().getResult();
                            if(null == result) continue;
                            result.print();
                    }
                }
                return null;
            }
        };
    }

    @Override
    public BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                String mode = config.get("mode").asText();
                for(Node item : source) {
                    switch (mode) {
                        case "map":
                            DataSet<Map<String, Object>> mapResult = item.getBatch().result();
                            if(null == mapResult) continue;
                            mapResult.print();
                            break;
                        case "json":
                            DataSet<Map<String, Object>> jsonResult = item.getBatch().result();
                            if(null == jsonResult) continue;
                            jsonResult.map(new MapFunction<Map<String,Object>, String>() {
                                @Override
                                public String map(Map<String, Object> value) throws Exception {
                                    return DPUtil.stringify(value);
                                }
                            }).name("Stringify").print();
                            break;
                        default:
                            DataSet<Map<String, Object>> result = item.getBatch().getResult();
                            if(null == result) continue;
                            result.print();
                    }
                }
                environment().fromCollection(Arrays.asList(DPUtil.buildMap(String.class, Object.class))).output(new EmptyOutput()).name(EmptyOutput.class.getSimpleName());
                return null;
            }
        };
    }
}
