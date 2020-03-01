package com.iisquare.fs.flink.plugins.olap.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Map;

public class AnchorNode extends Node {
    @Override
    public JsonNode run() throws Exception {
        return null;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                return current.getSource().iterator().next().getStream().result();
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                return current.getSource().iterator().next().getBatch().result();
            }
        };
    }
}
