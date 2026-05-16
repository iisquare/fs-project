package com.iisquare.fs.app.nlp.site;

import com.iisquare.fs.app.nlp.demo.SiteConfig;
import com.iisquare.fs.app.spark.demo.DemoConfig;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.*;
import org.apache.spark.sql.execution.datasources.jdbc.JDBCRollRelationProvider;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.Map;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.when;

public class SiteDimRegionLevel3Job {

    final static String level3Path = "C:\\Users\\Ouyang\\Desktop\\ok_data_level3.csv";

    public static void main(String[] args) {
        SparkConf config = DemoConfig.spark().setMaster("local").setAppName(SiteDimRegionLevel3Job.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        StructType schema = new StructType()
                .add("id", DataTypes.IntegerType, false)
                .add("pid", DataTypes.IntegerType, true)
                .add("deep", DataTypes.IntegerType, true)
                .add("name", DataTypes.StringType, true)
                .add("pinyin_prefix", DataTypes.StringType, true)
                .add("pinyin", DataTypes.StringType, true)
                .add("ext_id", DataTypes.StringType, true)
                .add("ext_name", DataTypes.StringType, true);
        Dataset<Row> csv = session.read()
                .format("csv")
                .option("header", "true")
                .option("dateFormat", "yyyy-MM-dd")
                .schema(schema)
                .load(level3Path);
        csv = csv.drop("name").withColumnRenamed("ext_name", "name"); // 将ext_name作为区域名称
        csv = csv.withColumnRenamed("pid", "parent_id");
        csv = csv.withColumnRenamed("pinyin_prefix", "letter");
        csv = csv.withColumnRenamed("ext_id", "code");
        csv = csv.withColumn("polygon", functions.lit(""));
        csv = csv.withColumn("description", functions.lit(""));
        csv = csv.withColumn("status",
                when(col("code").startsWith("71"), 2)
                .when(col("code").startsWith("81"), 2)
                .when(col("code").startsWith("91"), 2)
                .otherwise(1));
        Map<String, String> smap = SiteConfig.mysqlSink("fs_dim_region", 3);
        smap.put("roll", "id");
        csv.write().mode(SaveMode.Append).format(JDBCRollRelationProvider.class.getName()).options(smap).save();
        session.close();
    }
}
