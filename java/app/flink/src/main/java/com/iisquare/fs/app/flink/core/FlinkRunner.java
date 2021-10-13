package com.iisquare.fs.app.flink.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGRunner;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Map;

public class FlinkRunner extends DAGRunner {

    private Object flink = null;

    public FlinkRunner(JsonNode diagram, Map<String, Class<DAGNode>> nodes) {
        super(diagram, nodes);
    }

    public ExecutionEnvironment batch() {
        return (ExecutionEnvironment) flink;
    }

    public StreamExecutionEnvironment stream() {
        return (StreamExecutionEnvironment) flink;
    }

    @Override
    public void execute() throws Exception {
        String executeModel = diagram.at("/model").asText();
        switch (executeModel) {
            case "batch":
                flink = ExecutionEnvironment.getExecutionEnvironment();
                break;
            case "stream":
                flink = StreamExecutionEnvironment.getExecutionEnvironment();
                break;
        }
        super.execute();
        String jobName = diagram.at("/name").asText(this.getClass().getSimpleName());
        switch (executeModel) {
            case "batch":
                batch().execute(jobName);
                break;
            case "stream":
                stream().execute(jobName);
                break;
        }
    }
}
