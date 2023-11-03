package com.iisquare.fs.app.spark.util;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.elasticsearch.spark.sql.api.java.JavaEsSparkSQL;

public class SinkUtil {

    public static void es(Dataset<Row> dataset, String collection) {
        JavaEsSparkSQL.saveToEs(dataset, collection, ConfigUtil.es());
    }

    public static void mysql(Dataset<Row> dataset, final SaveMode mode, String table, int batchSize) {
        dataset.write().format("jdbc").mode(mode).options(ConfigUtil.mysqlSink(table, batchSize)).save();
    }

}
