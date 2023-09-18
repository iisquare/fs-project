package com.iisquare.fs.app.nlp.util;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class SourceUtil {

    public static Dataset<Row> mysql(SparkSession session, String sql, int fetchSize) {
        return session.read().format("jdbc").options(ConfigUtil.mysqlSource(sql, fetchSize)).load();
    }

}
