package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.BIApplication;
import com.iisquare.fs.web.bi.unit.DatasetUnit;
import com.iisquare.fs.web.bi.unit.MatrixUnit;
import com.iisquare.fs.web.bi.unit.TestUnit;
import com.iisquare.fs.web.bi.unit.VisualizeUnit;
import io.delta.sql.DeltaSparkSessionExtension;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.delta.catalog.DeltaCatalog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 采用临时视图处理SQL查询，用于Session隔离。
 * 每次请求创建新的Session，避免Session之间数据冲突。
 */
@Service
public class SparkService implements InitializingBean, Closeable, Serializable {

    private SparkSession spark = null;
    @Value("${spark.master:local}")
    private String sparkMaster;
    @Value("${spark.jarUri:}")
    private String sparkJarUri;

    @Override
    public void afterPropertiesSet() throws Exception {
        SparkSession.Builder builder = SparkSession.builder();
        builder.master(sparkMaster);
        builder.appName(BIApplication.class.getSimpleName());
        if (!DPUtil.empty(sparkJarUri)) builder.config("jars", sparkJarUri);
        if (!"local".equals(sparkMaster)) {
            builder.config("spark.dynamicAllocation.enabled", "true");
            builder.config("spark.dynamicAllocation.maxExecutors", "4");
            builder.config("spark.dynamicAllocation.initialExecutors", "1");
            builder.config("spark.dynamicAllocation.shuffleTracking.enabled", "true");
        }
        // 必须在创建Session之前进行配置，注意检查是否存在多个getOrCreate调用导致配置无效
        builder.config("spark.sql.extensions", DeltaSparkSessionExtension.class.getName());
        builder.config("spark.sql.catalog.spark_catalog", DeltaCatalog.class.getName());
        spark = builder.getOrCreate();
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        if (null != spark) spark.close();
    }

    public SparkSession session() {
        return spark;
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
