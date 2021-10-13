package com.iisquare.fs.app.spark.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGRunner;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

public class SparkRunner extends DAGRunner {

    private SparkSession spark = null;

    public SparkRunner(JsonNode diagram, Map<String, Class<DAGNode>> nodes) {
        super(diagram, nodes);
    }

    public SparkSession session() {
        return spark;
    }

    @Override
    public void execute() throws Exception {
        String appName = diagram.at("/name").asText(this.getClass().getSimpleName());
        SparkConf config = new SparkConf().setAppName(appName);
        if (DPUtil.empty(System.getenv("spark.master"))) config.setMaster("local");
        spark = SparkSession.builder().config(config).getOrCreate();
        try {
            super.execute();
        } finally {
            session().close();
        }
    }
}
