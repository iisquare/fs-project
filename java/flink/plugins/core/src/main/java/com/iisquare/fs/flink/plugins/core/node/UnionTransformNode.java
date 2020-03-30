package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Map;

public class UnionTransformNode extends Node {
    @Override
    public JsonNode run() throws Exception {
        return DPUtil.parseJSON(config);
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                DataStream<Map<String, Object>> union  = null;
                for (Node node : source) {
                    DataStream<Map<String, Object>> result = node.getStream().result();
                    if(null == result) continue;
                    if(null == union) {
                        union = result;
                        typeInfo = node.getTypeInfo();
                    } else {
                        union = union.union(result);
                    }
                }
                return union;
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                DataSet<Map<String, Object>> union  = null;
                for (Node node : source) {
                    DataSet<Map<String, Object>> result = node.getBatch().result();
                    if(null == result) continue;
                    if(null == union) {
                        union = result;
                        typeInfo = node.getTypeInfo();
                    } else {
                        union = union.union(result);
                    }
                }
                return union;
            }
        };
    }
}
