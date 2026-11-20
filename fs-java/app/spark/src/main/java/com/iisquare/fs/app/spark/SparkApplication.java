package com.iisquare.fs.app.spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        execute(args[0], null);
    }

    /**
     * 本地调试执行，默认限制 200 行；limit=0 表示不限制。
     */
    public static void debug(String uri) throws Exception {
        debug(uri, 200);
    }

    /**
     * 本地调试执行；limit=0 表示不限制，正数最大限制 1000 行。
     */
    public static void debug(String uri, int limit) throws Exception {
        execute(uri, Math.max(0, limit));
    }

    private static void execute(String uri, Integer debugLimit) throws Exception {
        JsonNode diagram = DAGUtil.loadDiagram(uri);
        if (null != debugLimit && diagram instanceof ObjectNode root) {
            root.put("debugLimit", debugLimit);
        }
        Map<String, Class<? extends DAGNode<?>>> nodes = DAGUtil.scanNodes(
                SparkApplication.class.getPackage().getName() + ".node",
                SparkApplication.class.getPackage().getName() + ".batch",
                SparkApplication.class.getPackage().getName() + ".stream",
                "com.iisquare.fs.ext.spark",
                DAGCore.class.getPackage().getName() + ".node",
                DAGCore.class.getPackage().getName() + ".config");
        String appName = diagram.at("/name").asText(SparkApplication.class.getSimpleName());
        SparkConf config = new SparkConf().setAppName(appName);
        if (DPUtil.empty(System.getenv("spark.master"))) config.setMaster("local");
        try (SparkSession session = SparkSession.builder().config(config).getOrCreate()) {
            SparkRunner runner = new SparkRunner(session, diagram, nodes);
            runner.execute();
            if (runner.isStreaming()) {
                // 流式作业持续运行，等待任意流查询终止
                session.streams().awaitAnyTermination();
            }
        }
    }

}
