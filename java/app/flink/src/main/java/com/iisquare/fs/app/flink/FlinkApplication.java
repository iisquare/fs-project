package com.iisquare.fs.app.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;

import java.util.Map;

public class FlinkApplication {

    public static void main(String... args) throws Exception {
        JsonNode diagram = DAGUtil.loadDiagram(args[0]);
        Map<String, Class<DAGNode>> nodes = DAGUtil.scanNodes(
                FlinkApplication.class.getPackage().getName() + ".node",
                FlinkApplication.class.getPackage().getName() + "." + diagram.at("/model").asText(),
                DAGCore.class.getPackage().getName() + ".node",
                DAGCore.class.getPackage().getName() + ".config");
        new FlinkRunner(diagram, nodes).execute();
    }

}
