package com.iisquare.fs.app.nlp.site;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.nlp.demo.SiteConfig;
import com.iisquare.fs.app.spark.demo.DemoConfig;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCRollRelationProvider;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.*;

public class SiteDimRegionMongoJob {

    public static JsonNode region(SparkSession session) {
        Map<String, String> options = SiteConfig.mysqlSource("select * from fs_dim_region", 100);
        Dataset<Row> dataset = session.read().format("jdbc").options(options).load();
        return SparkUtil.dataset2json(dataset);
    }

    public static List<String> parentNames(JsonNode region, String name, String parentId) {
        ObjectNode ids = DPUtil.json2object(region, "id");
        List<String> result = new ArrayList<>();
        result.add(name);
        while (ids.has(parentId)) {
            result.add(ids.at("/" + parentId + "/name").asText());
            String ancestorId = ids.at("/" + parentId + "/parent_id").asText();
            if (parentId.equals(ancestorId)) {
                throw new RuntimeException("duplicate region id: " + parentId);
            }
            parentId = ancestorId;
        }
        Collections.reverse(result);
        return result;
    }

    public static void main(String[] args) {
        SparkConf config = DemoConfig.spark().setMaster("local").setAppName(SiteDimRegionMongoJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Map<String, String> options = DemoConfig.mongo();
        options.put("database", "fs_project");
        options.put("collection", "fs_spider_region");
        StructType schema = DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("id", DataTypes.IntegerType, false),
                DataTypes.createStructField("code", DataTypes.StringType, false),
                DataTypes.createStructField("name", DataTypes.StringType, true),
                DataTypes.createStructField("full_name", DataTypes.StringType, true),
                DataTypes.createStructField("parent_id", DataTypes.IntegerType, true)
        ));
        Dataset<Row> dataset = session.read().format("mongodb").options(options).load();
        JsonNode region = region(session);
        ObjectNode codes = DPUtil.json2object(region, "code"); // 库中区划码可能存在重复
        dataset = dataset.flatMap((FlatMapFunction<Row, Row>) row -> {
            JsonNode json = SparkUtil.row2json(row.schema(), row);
            List<Row> result = new ArrayList<>();
            for (JsonNode item : json.at("/collect/list")) {
                item = DPUtil.parseJSON(item.asText());
                String code = item.at("/code").asText();
                if (DPUtil.empty(code) || code.contains("*")) break; // 排除异常数据
                if (codes.has(code)) continue; // 排除已清洗数据
                String name = item.at("/name").asText();
                if ("市辖区".equals(name)) continue;
                if (name.contains("直辖县")) continue; // 直辖县挂接在省级下与市平级
                if (name.endsWith("街道") || name.endsWith("镇")) continue;
                ObjectNode data = DPUtil.objectNode();
                data.put("code", code);
                data.put("name", name);
                String parentCode = json.at("/task_args/code").asText("");
                int parentId = codes.at("/" + parentCode + "/id").asInt();
                int deep = codes.at("/" + parentCode + "/deep").asInt();
                if (deep >= 2) continue; // 仅处理至区县级
                List<String> names = parentNames(region, name, String.valueOf(parentId));
                data.put("full_name", DPUtil.implode(" ", names));
                data.put("parent_id", parentId);
                data.put("id", DPUtil.parseInt(code.substring(0, 9)));
                result.add(SparkUtil.json2row(schema, data));
            }
            return result.iterator();
        }, RowEncoder.apply(schema));
        System.out.println("all count:" + dataset.count());
        dataset = dataset.where("parent_id > 0");
        dataset = dataset.withColumn("polygon", functions.lit(""));
        dataset = dataset.withColumn("description", functions.lit(""));
        dataset = dataset.withColumn("status", functions.lit(1));
        System.out.println("sink count:" + dataset.count());
        Map<String, String> smap = SiteConfig.mysqlSink("fs_dim_region", 3);
        smap.put("roll", "id");
        dataset.write().mode(SaveMode.Append).format(JDBCRollRelationProvider.class.getName()).options(smap).save();
        session.close();
    }
}
