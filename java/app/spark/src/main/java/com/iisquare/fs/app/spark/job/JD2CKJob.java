package com.iisquare.fs.app.spark.job;

import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SinkUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;

import java.util.Arrays;

public class JD2CKJob {
    public static void main(String[] args) {
        SparkConf config = ConfigUtil.spark().setAppName(JD2CKJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Dataset<Row> data = session.read().text("E:\\BaiduNetdiskDownload\\leakage\\jd.2020.txt");
        data = data.map((MapFunction<Row, Row>) row -> {
            String[] strings = row.getString(0).split("---", -1);
            Object[] arr = new Object[]{ strings[0], "", "", row.getString(0) };
            for (int i = 1; i < strings.length; i++) {
                String s = strings[i];
                if (s.indexOf("@") > 1) {
                    arr[1] = s;
                } else if (s.length() == 11) {
                    arr[2] = s;
                }
            }
            return RowFactory.create(arr);
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("name", DataTypes.StringType, false),
                DataTypes.createStructField("mail", DataTypes.StringType, false),
                DataTypes.createStructField("phone", DataTypes.StringType, false),
                DataTypes.createStructField("original", DataTypes.StringType, false)
        ))));
        SinkUtil.clickhouse(data, SaveMode.Append, "leakage.t_jd", 3000);
        session.close();
    }
}
