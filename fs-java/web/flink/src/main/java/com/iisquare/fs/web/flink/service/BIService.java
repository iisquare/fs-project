package com.iisquare.fs.web.flink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.FlinkApplication;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGExecution;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;
import com.iisquare.fs.web.flink.client.FlinkClusterClient;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.core.execution.JobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BIService {

    @Autowired
    FlinkService flinkService;
    private static Map<String, JobClient> clients = new ConcurrentHashMap<>();
    static Map<String, Class<? extends DAGNode<?>>> node;
    static Map<String, Class<? extends DAGNode<?>>> batch;
    static Map<String, Class<? extends DAGNode<?>>> stream;

    static {
        try {
            node = DAGUtil.scanNodes(
                    FlinkApplication.class.getPackage().getName() + ".node",
                    DAGCore.class.getPackage().getName() + ".node",
                    DAGCore.class.getPackage().getName() + ".config");
            batch = DAGUtil.scanNodes(
                    FlinkApplication.class.getPackage().getName() + ".batch");
            stream = DAGUtil.scanNodes(
                    FlinkApplication.class.getPackage().getName() + ".stream");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> clients() {
        ObjectNode result = DPUtil.objectNode();
        for (Map.Entry<String, JobClient> entry : clients.entrySet()) {
            result.replace(entry.getKey(), FlinkUtil.client(entry.getValue()));
        }
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> dag(JsonNode diagram) {
        JsonNode cluster = diagram.at("/cluster");
        String debugId = diagram.at("/debug").asText("");
        if ("remote".equals(cluster.at("/mode").asText(""))) {
            if (!DPUtil.empty(debugId)) {
                return ApiUtil.result(1504, "节点调试需在本地引擎执行，远程集群作业不支持逐节点调试", debugId);
            }
            String jobName = diagram.at("/name").asText("fs-flink-job");
            String diagramUrl = diagram.at("/diagramUrl").asText("");
            String arg = FlinkClusterClient.diagramArg(diagramUrl, diagram);
            return FlinkClusterClient.submit(cluster, jobName, arg, null);
        }
        String model = diagram.at("/model").asText("");
        if (!"batch".equals(model) && !"stream".equals(model)) {
            return ApiUtil.result(1503, "处理模式仅支持batch与stream", model);
        }
        Map<String, Class<? extends DAGNode<?>>> nodes = new LinkedHashMap<>();
        nodes.putAll(node);
        nodes.putAll(batch);
        nodes.putAll(stream);
        // 批处理与流处理共用 DataStream API，执行环境按处理模式选择
        FlinkRunner runner = new FlinkRunner(flinkService.environment(model), diagram, nodes);
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
            String jobName = diagram.at("/name").asText(this.getClass().getSimpleName());
            if ("batch".equals(model)) {
                // 批处理同步执行，执行结束即完成
                JobExecutionResult execute = runner.stream().execute(jobName);
                return ApiUtil.result(0, null, execute.getJobID().toString());
            }
            JobClient client = runner.stream().executeAsync(jobName);
            String jobId = client.getJobID().toString();
            clients.put(jobId, client);
            return ApiUtil.result(0, null, FlinkUtil.client(client));
        } catch (Exception e) {
            return ApiUtil.result(500, null, ApiUtil.getStackTrace(e));
        }
    }

    public Map<String, Object> clusterTest(JsonNode content) {
        return FlinkClusterClient.test(content);
    }

    public Map<String, Object> jobStatus(JsonNode content, String jobId) {
        if (!"remote".equals(content.at("/mode").asText("")) && clients.containsKey(jobId)) {
            JobClient client = clients.get(jobId);
            ObjectNode node = FlinkUtil.client(client);
            if (client.getJobExecutionResult().isDone()) {
                node.put("state", client.getJobExecutionResult().isCancelled() ? "CANCELED" : "FINISHED");
            } else {
                node.put("state", "RUNNING");
            }
            return ApiUtil.result(0, null, node);
        }
        return FlinkClusterClient.status(content, jobId);
    }

    public Map<String, Object> jobCancel(JsonNode content, String jobId) {
        if (!"remote".equals(content.at("/mode").asText("")) && clients.containsKey(jobId)) {
            clients.get(jobId).cancel();
            return ApiUtil.result(0, null, true);
        }
        return FlinkClusterClient.cancel(content, jobId);
    }

}
