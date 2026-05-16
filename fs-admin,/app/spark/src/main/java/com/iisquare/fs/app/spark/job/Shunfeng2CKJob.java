package com.iisquare.fs.app.spark.job;

import com.iisquare.fs.app.spark.demo.DemoConfig;
import com.iisquare.fs.app.spark.demo.DemoSink;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;

import java.util.Arrays;
import java.util.List;

public class Shunfeng2CKJob {
    public static void main(String[] args) {
        SparkConf config = DemoConfig.spark().setAppName(Shunfeng2CKJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Dataset<Row> data = session.read().text("E:\\BaiduNetdiskDownload\\leakage\\shunfeng.2020.sql");
        data = data.filter((FilterFunction<Row>) row -> row.getString(0).startsWith("INSERT "));
        data = data.map((MapFunction<Row, Row>) row -> {
            List<String> strings = DPUtil.matcher("VALUES \\(N'(.*?)', N'(.*?)', N'(.*?)', N'(.*?)', N'(.*?)', N'(.*?)'\\)", row.getString(0), true);
            return RowFactory.create(strings.subList(1, strings.size()).toArray(new Object[0]));
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("name", DataTypes.StringType, false),
                DataTypes.createStructField("phone", DataTypes.StringType, false),
                DataTypes.createStructField("province", DataTypes.StringType, false),
                DataTypes.createStructField("city", DataTypes.StringType, false),
                DataTypes.createStructField("dist", DataTypes.StringType, false),
                DataTypes.createStructField("addr", DataTypes.StringType, false)
        ))));
        DemoSink.clickhouse(data, SaveMode.Append, "leakage.t_shunfeng", 2000);
        session.close();
    }
}
