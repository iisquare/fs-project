package com.iisquare.fs.app.spark.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGRunner;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

public class SparkRunner extends DAGRunner {

    private SparkSession session;

    public SparkRunner(SparkSession session, JsonNode diagram, Map<String, Class<DAGNode>> nodes) {
        super(diagram, nodes);
        this.session = session;
    }

    public SparkSession session() {
        return session;
    }

}
