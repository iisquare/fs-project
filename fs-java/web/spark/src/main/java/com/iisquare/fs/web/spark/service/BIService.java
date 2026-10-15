package com.iisquare.fs.web.spark.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.SparkApplication;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGExecution;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;
import com.iisquare.fs.web.spark.client.SparkClusterClient;
import com.iisquare.fs.web.spark.unit.DatasetUnit;
import com.iisquare.fs.web.spark.unit.MatrixUnit;
import com.iisquare.fs.web.spark.unit.TestUnit;
import com.iisquare.fs.web.spark.unit.VisualizeUnit;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BIService {

    @Autowired
    SparkService sparkService;
    // 本地流式查询注册表：作业标识 -> 流查询，用于状态查询与取消
    private static Map<String, StreamingQuery> queries = new ConcurrentHashMap<>();
    static Map<String, Class<? extends DAGNode<?>>> node;
    static Map<String, Class<? extends DAGNode<?>>> batch;
    static Map<String, Class<? extends DAGNode<?>>> stream;

    static {
        try {
            node = DAGUtil.scanNodes(
                    SparkApplication.class.getPackage().getName() + ".node",
                    DAGCore.class.getPackage().getName() + ".node",
                    DAGCore.class.getPackage().getName() + ".config");
            batch = DAGUtil.scanNodes(
                    SparkApplication.class.getPackage().getName() + ".batch");
            stream = DAGUtil.scanNodes(
                    SparkApplication.class.getPackage().getName() + ".stream");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> dag(JsonNode diagram) {
        JsonNode cluster = diagram.at("/cluster");
        String debugId = diagram.at("/debug").asText("");
        if ("remote".equals(cluster.at("/mode").asText(""))) {
            if (!DPUtil.empty(debugId)) {
                return ApiUtil.result(1504, "节点调试需在本地引擎执行，远程集群作业不支持逐节点调试", debugId);
            }
            String jobName = diagram.at("/name").asText("fs-spark-job");
            String diagramUrl = diagram.at("/diagramUrl").asText("");
            String arg = SparkClusterClient.diagramArg(diagramUrl, diagram);
            return SparkClusterClient.submit(cluster, jobName, arg, null);
        }
        String model = diagram.at("/model").asText();
        if (!"batch".equals(model) && !"stream".equals(model)) {
            return ApiUtil.result(1503, "处理模式仅支持batch与stream", model);
        }
        Map<String, Class<? extends DAGNode<?>>> nodes = new LinkedHashMap<>();
        nodes.putAll(node);
        if ("batch".equals(model)) {
            nodes.putAll(batch);
        }
        if ("stream".equals(model)) {
            nodes.putAll(stream);
        }
        SparkSession session = sparkService.session().newSession();
        SparkRunner runner = new SparkRunner(session, diagram, nodes);
        try {
            if (!DPUtil.empty(debugId)) {
                runner.debug(debugId);
                DAGExecution execution = runner.execute();
                ObjectNode data = DPUtil.objectNode();
                data.put("debug", debugId);
                data.set("outputs", execution.outputs());
                return ApiUtil.result(0, null, data);
            }
            runner.execute();
            if ("stream".equals(model)) {
                // 流式作业异步运行，返回已启动的流查询标识供状态监控使用
                List<Map<String, Object>> list = new ArrayList<>();
                for (StreamingQuery query : session.streams().active()) {
                    queries.put(query.id().toString(), query);
                    list.add(query(query));
                }
                ObjectNode data = DPUtil.objectNode();
                data.put("model", model).put("name", diagram.at("/name").asText(""));
                data.replace("queries", DPUtil.toJSON(list));
                return ApiUtil.result(0, null, data);
            }
            return ApiUtil.result(0, null, diagram);
        } catch (Exception e) {
            return ApiUtil.result(500, null, ApiUtil.getStackTrace(e));
        }
    }

    /**
     * 流查询状态：运行中/失败/已停止
     */
    public Map<String, Object> query(StreamingQuery query) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", query.id().toString());
        result.put("name", query.name());
        result.put("runId", query.runId().toString());
        if (!query.isActive()) {
            result.put("driverState", null == query.exception() ? "CANCELED" : "FAILED");
            return result;
        }
        result.put("driverState", "RUNNING");
        return result;
    }

    public List<Double> random() {
        SparkSession session = sparkService.session().newSession();
        return TestUnit.random(session);
    }

    public Map<String, Object> dataset(JsonNode options) {
        SparkSession session = sparkService.session().newSession();
        return DatasetUnit.dataset(session, options);
    }

    public Map<String, Object> visualize(JsonNode dataset, JsonNode options, JsonNode levels) {
        SparkSession session = sparkService.session().newSession();
        return VisualizeUnit.visualize(session, dataset, options, levels);
    }

    public Map<String, Object> matrix(JsonNode dataset, JsonNode options) {
        SparkSession session = sparkService.session().newSession();
        return MatrixUnit.matrix(session, dataset, options);
    }

    public Map<String, Object> clusterTest(JsonNode content) {
        return SparkClusterClient.test(content);
    }

    public Map<String, Object> jobStatus(JsonNode content, String jobId) {
        if (!"remote".equals(content.at("/mode").asText("")) && queries.containsKey(jobId)) {
            return ApiUtil.result(0, null, query(queries.get(jobId)));
        }
        return SparkClusterClient.status(content, jobId);
    }

    public Map<String, Object> jobCancel(JsonNode content, String jobId) {
        if (!"remote".equals(content.at("/mode").asText("")) && queries.containsKey(jobId)) {
            StreamingQuery query = queries.get(jobId);
            try {
                query.stop();
            } catch (Exception e) {
                return ApiUtil.result(5001, "停止流查询失败", e.getMessage());
            }
            return ApiUtil.result(0, null, query(query));
        }
        return SparkClusterClient.cancel(content, jobId);
    }

}
