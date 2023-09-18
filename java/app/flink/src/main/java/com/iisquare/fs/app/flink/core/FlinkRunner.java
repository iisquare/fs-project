package com.iisquare.fs.app.flink.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGRunner;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Map;

public class FlinkRunner extends DAGRunner {

    private Object environment;

    public FlinkRunner(Object environment, JsonNode diagram, Map<String, Class<DAGNode>> nodes) {
        super(diagram, nodes);
        this.environment = environment;
    }

    public ExecutionEnvironment batch() {
        return (ExecutionEnvironment) environment;
    }

    public StreamExecutionEnvironment stream() {
        return (StreamExecutionEnvironment) environment;
    }

}
