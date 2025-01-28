package com.iisquare.fs.app.nlp.job;

import com.hankcs.hanlp.HanLP;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCRollRelationProvider;

import java.util.Map;

public class PinyinJob {
    public static void main(String[] args) {
        SparkSession session = SparkSession.builder().appName("jdbc-roll-pinyin").master("local").getOrCreate();
        Map<String, String> options = ConfigUtil.mysql();
        options.put("roll", "id");
        options.put("batchSize", "500");
        options.put("query", "select id, name, pinyin, description from fs_project.fs_member_dictionary where pinyin = ''");
        Dataset<Row> dataset = session.read().format(JDBCRollRelationProvider.class.getName()).options(options).load();
        dataset = dataset.map((MapFunction<Row, Row>) row -> {
            String pinyin = HanLP.convertToPinyinString(row.getAs("name"), "", false);
            return RowFactory.create(row.get(0), row.get(1), pinyin, row.get(3));
        }, dataset.encoder());
        Map<String, String> smap = ConfigUtil.mysqlSink("fs_project.fs_member_dictionary", 200);
        smap.put("roll", "id");
        dataset.write().mode(SaveMode.Append).format(JDBCRollRelationProvider.class.getName()).options(smap).save();
        session.close();
    }
}
