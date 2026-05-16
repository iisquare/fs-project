package com.iisquare.fs.app.nlp.site;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hankcs.hanlp.HanLP;
import com.iisquare.fs.app.nlp.demo.SiteConfig;
import com.iisquare.fs.app.spark.demo.DemoConfig;
import com.iisquare.fs.app.spark.util.SparkUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCRollRelationProvider;

import java.util.*;

public class SiteDimRegionPinyinJob {

    public static void main(String[] args) {
        SparkConf config = DemoConfig.spark().setMaster("local").setAppName(SiteDimRegionPinyinJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Map<String, String> options = SiteConfig.mysql();
        options.put("roll", "id");
        options.put("batchSize", "500");
        options.put("query", "select id, name, letter, pinyin, polygon, description from fs_dim_region where letter = ''");
        Dataset<Row> dataset = session.read().format(JDBCRollRelationProvider.class.getName()).options(options).load();
        dataset = dataset.map((MapFunction<Row, Row>) row -> {
            ObjectNode json = SparkUtil.row2json(row.schema(), row);
            String pinyin = HanLP.convertToPinyinString(row.getAs("name"), " ", false);
            json.put("pinyin", pinyin);
            json.put("letter", pinyin.substring(0, 1));
            return SparkUtil.json2row(row.schema(), json);
        }, dataset.encoder());
        System.out.println("count: " + dataset.count());
        dataset.show();
        Map<String, String> smap = SiteConfig.mysqlSink("fs_dim_region", 200);
        smap.put("roll", "id");
        dataset.write().mode(SaveMode.Append).format(JDBCRollRelationProvider.class.getName()).options(smap).save();
        session.close();
    }
}
