package com.iisquare.fs.app.spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

public class SparkApplication {

    public static void main(String... args) throws Exception {
        JsonNode diagram = DAGUtil.loadDiagram(args[0]);
        Map<String, Class<DAGNode>> nodes = DAGUtil.scanNodes(
                SparkApplication.class.getPackage().getName() + ".node",
                SparkApplication.class.getPackage().getName() + "." + diagram.at("/model").asText(),
                DAGCore.class.getPackage().getName() + ".node",
                DAGCore.class.getPackage().getName() + ".config");
        String appName = diagram.at("/name").asText(SparkApplication.class.getSimpleName());
        SparkConf config = new SparkConf().setAppName(appName);
        if (DPUtil.empty(System.getenv("spark.master"))) config.setMaster("local");
        try (SparkSession session = SparkSession.builder().config(config).getOrCreate()) {
            new SparkRunner(session, diagram, nodes).execute();
        }
    }

}
