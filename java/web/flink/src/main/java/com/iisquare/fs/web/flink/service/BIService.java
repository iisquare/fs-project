package com.iisquare.fs.web.flink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.FlinkApplication;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.core.execution.JobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BIService {

    @Autowired
    private FlinkService flinkService;
    private static Map<String, JobClient> clients = new ConcurrentHashMap<>();
    static Map<String, Class<DAGNode>> node;
    static Map<String, Class<DAGNode>> batch;
    static Map<String, Class<DAGNode>> stream;

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
        Configuration config = new Configuration();
        config.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        String model = diagram.at("/model").asText("");
        FlinkRunner runner;
        Map<String, Class<DAGNode>> nodes = new LinkedHashMap<>();
        nodes.putAll(node);
        switch (model) {
            case "batch":
                nodes.putAll(batch);
                runner = new FlinkRunner(flinkService.batch(), diagram, nodes);
                break;
            case "stream":
                nodes.putAll(stream);
                runner = new FlinkRunner(flinkService.stream(), diagram, nodes);
                break;
            default:
                return ApiUtil.result(1503, "处理类型不支持", model);
        }
        try {
            runner.execute();
            String jobName = diagram.at("/name").asText(this.getClass().getSimpleName());
            switch (model) {
                case "batch":
                    JobExecutionResult execute = runner.batch().execute(jobName);
                    return ApiUtil.result(0, null, execute.getJobID().toString());
                case "stream":
                    JobClient client = runner.stream().executeAsync(jobName);
                    String jobId = client.getJobID().toString();
                    clients.put(jobId, client);
                    return ApiUtil.result(0, null, FlinkUtil.client(client));
                default:
                    return ApiUtil.result(1503, "处理类型不支持", model);
            }
        } catch (Exception e) {
            return ApiUtil.result(500, null, ApiUtil.getStackTrace(e));
        }
    }

}
