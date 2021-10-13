package com.iisquare.fs.app.spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;

import java.util.Map;

public class SparkApplication {

    public static void main(String... args) throws Exception {
        JsonNode diagram = DAGUtil.loadDiagram(args[0]);
        Map<String, Class<DAGNode>> nodes = DAGUtil.scanNodes(
                SparkApplication.class.getPackage().getName() + ".node",
                SparkApplication.class.getPackage().getName() + "." + diagram.at("/model").asText(),
                DAGCore.class.getPackage().getName() + ".node",
                DAGCore.class.getPackage().getName() + ".config");
        new SparkRunner(diagram, nodes).execute();
    }

}
