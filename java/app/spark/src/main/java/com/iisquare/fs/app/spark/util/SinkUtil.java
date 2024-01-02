package com.iisquare.fs.app.spark.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.elasticsearch.spark.sql.api.java.JavaEsSparkSQL;

import java.util.Map;

public class SinkUtil {

    public static void elasticsearch(Dataset<Row> dataset, String collection, String idField, int batchSize) {
        Map<String, String> config = ConfigUtil.elasticsearch();
        if (!DPUtil.empty(idField)) {
            config.put("es.mapping.id", idField);
        }
        config.put("es.batch.size.entries", String.valueOf(batchSize));
        JavaEsSparkSQL.saveToEs(dataset, collection, config);
    }

    public static void mysql(Dataset<Row> dataset, final SaveMode mode, String table, int batchSize) {
        dataset.write().format("jdbc").mode(mode).options(ConfigUtil.mysqlSink(table, batchSize)).save();
    }

    public static void clickhouse(Dataset<Row> dataset, final SaveMode mode, String table, int batchSize) {
        dataset.write().format("jdbc").mode(mode).options(ConfigUtil.clickhouseSink(table, batchSize)).save();
    }

}
