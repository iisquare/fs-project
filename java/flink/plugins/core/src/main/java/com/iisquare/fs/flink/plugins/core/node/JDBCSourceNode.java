package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.plugins.core.input.JDBCInput;
import com.iisquare.fs.flink.plugins.core.source.JDBCSource;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Map;

public class JDBCSourceNode extends Node {

    @Override
    public JsonNode run() throws Exception {
        return DPUtil.parseJSON(config);
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                return environment().addSource(new JDBCSource(current.getConfig()), current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                return environment().createInput(new JDBCInput(DPUtil.convertJSON(config, Map.class))).name(current.getClass().getSimpleName());
            }
        };
    }

}
