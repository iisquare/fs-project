package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.plugins.core.input.HBaseInput;
import com.iisquare.fs.flink.plugins.core.source.HBaseSource;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Map;

public class HBaseSourceNode extends Node {

    private String quorum, znode, zkRetry, clientRetry, tableName, fields, startRow, endRow;

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        quorum = config.get("quorum").asText(); // ZK节点列表，以英文逗号分隔
        znode = config.get("znode").asText(); // 所在ZK中的目录结构
        zkRetry = config.get("zkRetry").asText();
        clientRetry = config.get("clientRetry").asText();
        tableName = config.get("tableName").asText();
        fields = config.get("fields").asText();
        startRow = config.get("startRow").asText();
        endRow = config.get("endRow").asText();
        JsonNode dateFormat = DateFormatNode.fromNode(source);
        if(null != dateFormat) {
            tableName = tableName.replaceFirst("\\{datetime\\}", dateFormat.get("formatted").asText());
        }
        return config;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                return environment().addSource(new HBaseSource(quorum, znode, zkRetry, clientRetry, tableName, fields, startRow, endRow))
                        .setParallelism(parallelism)
                        .name(current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                return environment().createInput(new HBaseInput(quorum, znode, zkRetry, clientRetry, tableName, fields, startRow, endRow))
                        .setParallelism(parallelism)
                        .name(current.getClass().getSimpleName());
            }
        };
    }
}
