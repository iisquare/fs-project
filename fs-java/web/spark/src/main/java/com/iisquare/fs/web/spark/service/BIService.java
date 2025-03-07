package com.iisquare.fs.web.spark.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.SparkApplication;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;
import com.iisquare.fs.web.spark.unit.DatasetUnit;
import com.iisquare.fs.web.spark.unit.MatrixUnit;
import com.iisquare.fs.web.spark.unit.TestUnit;
import com.iisquare.fs.web.spark.unit.VisualizeUnit;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BIService {

    @Autowired
    private SparkService sparkService;
    static Map<String, Class<DAGNode>> node;
    static Map<String, Class<DAGNode>> batch;
    static Map<String, Class<DAGNode>> stream;

    static {
        try {
            node = DAGUtil.scanNodes(
                    SparkApplication.class.getPackage().getName() + ".node",
                    DAGCore.class.getPackage().getName() + ".node",
                    DAGCore.class.getPackage().getName() + ".config");
            batch = DAGUtil.scanNodes(
                    SparkApplication.class.getPackage().getName() + ".batch");
            stream = DAGUtil.scanNodes(
                    SparkApplication.class.getPackage().getName() + ".stream");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> dag(JsonNode diagram) {
        String model = diagram.at("/model").asText();
        Map<String, Class<DAGNode>> nodes = new LinkedHashMap<>();
        nodes.putAll(node);
        if ("batch".equals(model)) {
            nodes.putAll(batch);
        }
        if ("stream".equals(model)) {
            nodes.putAll(stream);
        }
        SparkSession session = sparkService.session().newSession();
        SparkRunner runner = new SparkRunner(session, diagram, nodes);
        try {
            runner.execute();
            return ApiUtil.result(0, null, diagram);
        } catch (Exception e) {
            return ApiUtil.result(500, null, ApiUtil.getStackTrace(e));
        }
    }

    public List<Double> random() {
        SparkSession session = sparkService.session().newSession();
        return TestUnit.random(session);
    }

    public Map<String, Object> dataset(JsonNode options) {
        SparkSession session = sparkService.session().newSession();
        return DatasetUnit.dataset(session, options);
    }

    public Map<String, Object> visualize(JsonNode dataset, JsonNode options, JsonNode levels) {
        SparkSession session = sparkService.session().newSession();
        return VisualizeUnit.visualize(session, dataset, options, levels);
    }

    public Map<String, Object> matrix(JsonNode dataset, JsonNode options) {
        SparkSession session = sparkService.session().newSession();
        return MatrixUnit.matrix(session, dataset, options);
    }

}
