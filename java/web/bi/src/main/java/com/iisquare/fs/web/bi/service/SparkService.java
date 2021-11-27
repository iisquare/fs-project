package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.web.bi.BIApplication;
import com.iisquare.fs.web.bi.unit.DatasetUnit;
import com.iisquare.fs.web.bi.unit.MatrixUnit;
import com.iisquare.fs.web.bi.unit.TestUnit;
import com.iisquare.fs.web.bi.unit.VisualizeUnit;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 采用临时视图处理SQL查询，用于Session隔离。
 * 每次请求创建新的Session，避免Session之间数据冲突。
 */
@Service
public class SparkService implements InitializingBean, Closeable {

    private SparkSession spark = null;
    @Value("${spark.master:local}")
    private String sparkMaster;

    @Override
    public void afterPropertiesSet() throws Exception {
        SparkConf config = new SparkConf().setAppName(BIApplication.class.getSimpleName());
        config.setMaster(sparkMaster);
        spark = SparkSession.builder().config(config).getOrCreate();
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        if (null != spark) spark.close();
    }

    public List<Double> random() {
        SparkSession session = spark.newSession();
        return TestUnit.random(session);
    }

    public Map<String, Object> dataset(JsonNode options) {
        SparkSession session = spark.newSession();
        return DatasetUnit.dataset(session, options);
    }

    public Map<String, Object> visualize(JsonNode dataset, JsonNode options, JsonNode levels) {
        SparkSession session = spark.newSession();
        return VisualizeUnit.visualize(session, dataset, options, levels);
    }

    public Map<String, Object> matrix(JsonNode dataset, JsonNode options) {
        SparkSession session = spark.newSession();
        return MatrixUnit.matrix(session, dataset, options);
    }

}
