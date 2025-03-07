package com.iisquare.fs.app.nlp.job;

import com.iisquare.fs.app.spark.helper.AIHelper;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.FinalClose;
import com.mysql.cj.jdbc.Driver;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCRollRelationProvider;
import org.apache.spark.sql.types.StructType;

import java.util.LinkedHashMap;
import java.util.Map;

public class GPT4QA {
    public static void main(String[] args) {
        int parallel = 25;
        SparkSession session = SparkSession.builder().appName("gpt-qa").master("local[" + parallel + "]").getOrCreate();
        Map<String, String> options = new LinkedHashMap<>();
        options.put("url", "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
        options.put("driver", Driver.class.getName());
        options.put("user", "root");
        options.put("password", "admin888");
        options.put("roll", "序号");
        options.put("batchSize", "10000");
        options.put("query", "select * from `生成内容测试题` where `回答` is null or `回答` = ''");
        Dataset<Row> dataset = session.read().format(JDBCRollRelationProvider.class.getName()).options(options).load();
        StructType schema = dataset.encoder().schema();
        dataset = dataset.repartition(parallel).map((MapFunction<Row, Row>) row -> {
            AIHelper aiHelper = FinalClose.instance().register(AIHelper.class, k -> new AIHelper());
            String answer = aiHelper.qa(row.getAs("问题"));
            return SparkUtil.row(schema, row.get(0), row.get(1), row.get(2), row.get(3), answer);
        }, dataset.encoder());
        Map<String, String> smap = ConfigUtil.mysqlSink("`生成内容测试题`", 1);
        smap.put("roll", "序号");
        dataset.write().mode(SaveMode.Append).format(JDBCRollRelationProvider.class.getName()).options(smap).save();
        session.close();
    }
}
