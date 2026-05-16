package com.iisquare.fs.app.nlp.site;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.nlp.demo.SiteConfig;
import com.iisquare.fs.app.spark.demo.DemoConfig;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCRollRelationProvider;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.Map;

public class SiteDimRegionGeoJob {

    final static String geoPath = "C:\\Users\\Ouyang\\Desktop\\ok_geo.csv";

    public static void main(String[] args) {
        SparkConf config = DemoConfig.spark().setMaster("local").setAppName(SiteDimRegionGeoJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        StructType schema = new StructType()
                .add("id", DataTypes.IntegerType, false)
                .add("pid", DataTypes.IntegerType, true)
                .add("deep", DataTypes.IntegerType, true)
                .add("name", DataTypes.StringType, true)
                .add("ext_path", DataTypes.StringType, true)
                .add("geo", DataTypes.StringType, true)
                .add("polygon", DataTypes.StringType, true);
        Dataset<Row> csv = session.read()
                .format("csv")
                .option("header", "true")
                .option("dateFormat", "yyyy-MM-dd")
                .schema(schema)
                .load(geoPath);
        csv = csv.withColumnRenamed("pid", "parent_id");
        csv = csv.withColumnRenamed("ext_path", "full_name");
        csv = csv.withColumn("description", functions.lit(""));
        schema = csv.schema()
                .add("longitude", DataTypes.StringType, true)
                .add("latitude", DataTypes.StringType, true);
        StructType finalSchema = schema;
        csv = csv.map((MapFunction<Row, Row>) row -> {
            ObjectNode json = SparkUtil.row2json(row.schema(), row);
            String geo = json.at("/geo").asText();
            String polygon = json.at("/polygon").asText();
            if ("EMPTY".equals(polygon)) { // polygon
                json.put("polygon", "");
            }
            String[] strings = DPUtil.explode(" ", geo);
            if (strings.length == 2) {
                json.put("longitude", strings[0]);
                json.put("latitude", strings[1]);
            } else {
                json.put("longitude", "");
                json.put("latitude", "");
            }
            return SparkUtil.json2row(finalSchema, json);
        }, RowEncoder.apply(schema));
        csv = csv.drop("parent_id", "deep", "name", "geo");
        Map<String, String> smap = SiteConfig.mysqlSink("fs_dim_region", 3);
        smap.put("roll", "id");
        csv.write().mode(SaveMode.Append).format(JDBCRollRelationProvider.class.getName()).options(smap).save();
        session.close();
    }
}
