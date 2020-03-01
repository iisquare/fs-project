package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.plugins.core.output.HBaseOutput;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Map;

public class HBaseSinkNode extends Node {

    private String quorum, znode, zkRetry, clientRetry, tableName, idField, tableField;

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        quorum = config.get("quorum").asText(); // ZK节点列表，以英文逗号分隔
        znode = config.get("znode").asText(); // 所在ZK中的目录结构
        zkRetry = config.get("zkRetry").asText();
        clientRetry = config.get("clientRetry").asText();
        tableName = config.get("tableName").asText();
        idField = config.get("idField").asText();
        tableField = config.get("tableField").asText();
        return config;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                for (Node node : source) {
                    node.getStream().result().writeUsingOutputFormat(new HBaseOutput(
                        quorum, znode, zkRetry, clientRetry, tableName, idField, tableField, node.getTypeInfo().getFieldNames()
                    )).name(current.getClass().getSimpleName());
                }
                return null;
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                for (Node node : source) {
                    node.getBatch().result().output(new HBaseOutput(
                        quorum, znode, zkRetry, clientRetry, tableName, idField, tableField, node.getTypeInfo().getFieldNames()
                    )).name(current.getClass().getSimpleName());
                }
                return null;
            }
        };
    }
}
