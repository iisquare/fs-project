package com.iisquare.fs.app.spark.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGRunner;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;

import java.util.List;
import java.util.Map;

public class SparkRunner extends DAGRunner {

    private SparkSession session;

    public SparkRunner(SparkSession session, JsonNode diagram, Map<String, Class<? extends DAGNode<?>>> nodes) {
        super(diagram, nodes);
        this.session = session;
    }

    public SparkSession session() {
        return session;
    }

    /**
     * 是否流式计算（Spark Structured Streaming）
     */
    public boolean isStreaming() {
        return "stream".equals(diagram.at("/model").asText(""));
    }

    /**
     * 调试输出：对Dataset类型采集Schema与前若干行数据，便于可视化查看节点结果
     */
    @Override
    protected JsonNode describe(DAGNode<?> node) {
        Object result = node.result();
        if (result instanceof Dataset) {
            Dataset<?> dataset = (Dataset<?>) result;
            ObjectNode json = DPUtil.objectNode();
            json.put("type", "Dataset");
            ObjectNode schema = json.putObject("schema");
            for (StructField field : dataset.schema().fields()) {
                schema.put(field.name(), field.dataType().catalogString());
            }
            int limit = 20;
            json.put("limit", limit);
            try {
                ArrayNode rows = json.putArray("rows");
                List<String> list = dataset.limit(limit).toJSON().collectAsList();
                for (String row : list) {
                    rows.add(DPUtil.parseJSON(row));
                }
            } catch (Exception e) {
                json.put("preview", e.getMessage());
            }
            return json;
        }
        return super.describe(node);
    }

}
