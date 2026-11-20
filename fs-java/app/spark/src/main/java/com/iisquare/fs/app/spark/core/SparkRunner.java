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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

public class SparkRunner extends DAGRunner {

    public static List<String> limitSources = Arrays.asList(
            "MongoSource",
            "JDBCSource",
            "ElasticsearchSource",
            "Neo4jSource",
            "OntologySource",
            "ExcelSource",
            "Jar"
    );

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

    public int debugLimit() {
        int defaultLimit = isDebug() ? 20 : 0;
        int limit = diagram.at("/debugLimit").asInt(defaultLimit);
        return Math.max(0, Math.min(limit, 1000));
    }

    @Override
    public com.iisquare.fs.base.dag.core.DAGExecution execute() {
        if ((isDebug() || diagram.has("debugLimit")) && diagram instanceof ObjectNode root) {
            JsonNode nodes = root.get("nodes");
            if (null != nodes && nodes.isArray()) {
                int limit = debugLimit();
                for (JsonNode item : nodes) {
                    if (!item.isObject()) continue;
                    String type = item.at("/type").asText();
                    if (!limitSources.contains(type)) continue;
                    ObjectNode node = (ObjectNode) item;
                    ObjectNode options = node.withObject("options");
                    if (limit > 0) {
                        options.put("debugLimit", limit);
                    } else {
                        options.remove("debugLimit");
                    }
                }
            }
        }
        return super.execute();
    }

    /**
     * 调试输出：对Dataset类型采集Schema与前若干行数据，便于可视化查看节点结果
     */
    @Override
    protected JsonNode describe(DAGNode<?> node) {
        Object result = node.result();
        if (result instanceof Dataset<?> dataset) {
            ObjectNode json = DPUtil.objectNode();
            json.put("type", "Dataset");
            ObjectNode schema = json.putObject("schema");
            for (StructField field : dataset.schema().fields()) {
                schema.put(field.name(), field.dataType().catalogString());
            }
            int limit = debugLimit();
            json.put("limit", limit);
            try {
                ArrayNode rows = json.putArray("rows");
                Dataset<?> preview = limit > 0 ? dataset.limit(limit) : dataset;
                List<String> list = preview.toJSON().collectAsList();
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
