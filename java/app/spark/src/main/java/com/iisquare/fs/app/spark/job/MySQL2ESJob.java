package com.iisquare.fs.app.spark.job;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SinkUtil;
import com.iisquare.fs.app.spark.util.SourceUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.TypeUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class MySQL2ESJob {
    public static void main(String[] args) {
        SparkConf config = ConfigUtil.spark().setAppName(MySQL2ESJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        String sql = "select base_id, base_name from dwd_company_info";
        Dataset<Row> source = SourceUtil.mysql(session, sql, Integer.MIN_VALUE);
        Dataset<String> dataset = source.map((MapFunction<Row, String>) row -> {
            ObjectNode node = DPUtil.objectNode();
            node.put("id", TypeUtil.string(row.getAs("base_id")));
            String name = row.getAs("base_name");
            node.put("name_text", name);
            node.put("name_keyword", name);
            node.put("name_wildcard", name);
            return DPUtil.stringify(node);
        }, Encoders.STRING());
        SinkUtil.elasticsearch(dataset.toJavaRDD(), "fs_demo_test", "id", 1000);
        session.close();
    }
}
