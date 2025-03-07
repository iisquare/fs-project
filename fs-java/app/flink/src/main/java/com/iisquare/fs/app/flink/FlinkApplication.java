package com.iisquare.fs.app.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Map;

public class FlinkApplication {

    public static void main(String... args) throws Exception {
        JsonNode diagram = DAGUtil.loadDiagram(args[0]);
        String model = diagram.at("/model").asText("");
        Map<String, Class<DAGNode>> nodes = DAGUtil.scanNodes(
                FlinkApplication.class.getPackage().getName() + ".node",
                FlinkApplication.class.getPackage().getName() + "." + model,
                DAGCore.class.getPackage().getName() + ".node",
                DAGCore.class.getPackage().getName() + ".config");
        FlinkRunner runner;
        switch (model) {
            case "batch":
                runner = new FlinkRunner(ExecutionEnvironment.getExecutionEnvironment(), diagram, nodes);
                break;
            case "stream":
                runner = new FlinkRunner(StreamExecutionEnvironment.getExecutionEnvironment(), diagram, nodes);
                break;
            default:
                return;
        }
        runner.execute();
        String jobName = diagram.at("/name").asText(FlinkApplication.class.getSimpleName());
        switch (model) {
            case "batch":
                runner.batch().execute(jobName);
                break;
            case "stream":
                runner.stream().execute(jobName);
                break;
        }
    }

}
