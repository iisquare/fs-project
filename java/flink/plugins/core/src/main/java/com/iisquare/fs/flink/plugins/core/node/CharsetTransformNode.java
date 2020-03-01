package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class CharsetTransformNode extends Node {

    Node node;
    String fromCharset, toEncoding;

    @Override
    public JsonNode run() throws Exception {
        node = source.iterator().next();
        JsonNode config = DPUtil.parseJSON(this.config);
        fromCharset = config.get("fromCharset").asText();
        toEncoding = config.get("toEncoding").asText();
        return config;

    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                return node.getStream().result().map(map()).name(current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                return node.getBatch().result().map(map()).name(current.getClass().getSimpleName());
            }
        };
    }

    private MapFunction map() {
        String fromCharset = this.fromCharset, toEncoding = this.toEncoding;
        return new MapFunction<Map<String,Object>, Map<String,Object>>() {
            @Override
            public Map<String,Object> map(Map<String, Object> item) {
                for (Map.Entry<String, Object> entry : item.entrySet()) {
                    Object value = entry.getValue();
                    if (null == value) continue;
                    if (value instanceof String) {
                        try {
                            value = new String(((String) value).getBytes(fromCharset), toEncoding);
                        } catch (UnsupportedEncodingException e) {
                            continue;
                        }
                        item.put(entry.getKey(), value);
                    }
                }
                return item;
            }
        };
    }
}
