package com.iisquare.fs.app.spark.demo;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;
import org.elasticsearch.spark.sql.api.java.JavaEsSparkSQL;

import java.util.Map;

public class DemoSink {

    public static void elasticsearch(Dataset<Row> dataset, String collection, String idField, int batchSize) {
        Map<String, String> config = DemoConfig.elasticsearch();
        if (!DPUtil.empty(idField)) {
            config.put("es.mapping.id", idField);
        }
        config.put("es.batch.size.entries", String.valueOf(batchSize));
        JavaEsSparkSQL.saveToEs(dataset, collection, config);
    }

    public static void elasticsearch(JavaRDD<String> rdd, String collection, String idField, int batchSize) {
        Map<String, String> config = DemoConfig.elasticsearch();
        if (!DPUtil.empty(idField)) {
            config.put("es.mapping.id", idField);
        }
        config.put("es.batch.size.entries", String.valueOf(batchSize));
        JavaEsSpark.saveJsonToEs(rdd, collection, config);
    }

    public static void mysql(Dataset<Row> dataset, final SaveMode mode, String table, int batchSize) {
        dataset.write().format("jdbc").mode(mode).options(DemoConfig.mysqlSink(table, batchSize)).save();
    }

    public static void clickhouse(Dataset<Row> dataset, final SaveMode mode, String table, int batchSize) {
        dataset.write().format("jdbc").mode(mode).options(DemoConfig.clickhouseSink(table, batchSize)).save();
    }

}
