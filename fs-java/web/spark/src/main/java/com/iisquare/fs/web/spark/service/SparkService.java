package com.iisquare.fs.web.spark.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.spark.SparkApplication;
import io.delta.sql.DeltaSparkSessionExtension;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.delta.catalog.DeltaCatalog;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * 采用临时视图处理SQL查询，用于Session隔离。
 * 每次请求创建新的Session，避免Session之间数据冲突。
 */
@Service
public class SparkService implements InitializingBean, DisposableBean, Serializable {

    private SparkSession session = null;
    @Value("${spark.master:local}")
    private String sparkMaster;
    @Value("${spark.jarUri:}")
    private String sparkJarUri;

    @Override
    public void afterPropertiesSet() throws Exception {
        SparkSession.Builder builder = SparkSession.builder();
        builder.master(sparkMaster);
        builder.appName(SparkApplication.class.getSimpleName());
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
        session = builder.getOrCreate();
    }

    @Override
    public void destroy() throws Exception {
        if (null != session) session.close();
    }

    public SparkSession session() {
        return session;
    }

}
