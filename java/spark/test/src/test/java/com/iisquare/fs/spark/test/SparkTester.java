package com.iisquare.fs.spark.test;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SparkTester {

    @Test
    public void streamingTest() {
        // 在WSL下执行nc -lk 9999命令，等待测试程序连接
        SparkSession spark = SparkSession.builder().appName("test-structured-streaming").master("local").getOrCreate();
        Dataset<Row> lines = spark.readStream()
                .format("socket").option("host", "localhost").option("port", 9999).load();
        Dataset<String> words = lines
                .as(Encoders.STRING())
                .flatMap((FlatMapFunction<String, String>) x -> Arrays.asList(x.split(" ")).iterator(), Encoders.STRING());
        Dataset<Row> wordCounts = words.groupBy("value").count();
        StreamingQuery query = wordCounts.writeStream()
                .outputMode("complete").format("console").start();
        try {
            query.awaitTermination();
        } catch (StreamingQueryException e) {
            e.printStackTrace();
        }
        spark.close();
    }

    @Test
    public void randomTest() {
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.println(i + "." + j + "->" + DPUtil.random(0, i));
            }
        }
    }

    @Test
    public void sqlTest() {
        SparkSession spark = SparkSession.builder().appName("test-sql").master("local").getOrCreate();
        List<Row> rows = Arrays.asList(
                RowFactory.create(1, "a", DPUtil.random(0, 100)),
                RowFactory.create(2, "a", DPUtil.random(0, 100)),
                RowFactory.create(3, "b", DPUtil.random(0, 100)),
                RowFactory.create(4, "c", DPUtil.random(0, 100)),
                RowFactory.create(5, "f", DPUtil.random(0, 100)),
                RowFactory.create(6, "d", DPUtil.random(0, 100)),
                RowFactory.create(6, "f", DPUtil.random(0, 100)),
                RowFactory.create(6, "e", DPUtil.random(0, 100))
        );
        Dataset<Row> frame = spark.createDataFrame(rows, new StructType()
                .add("id", DataTypes.IntegerType, false)
                .add("name", DataTypes.StringType, false)
                .add("age", DataTypes.IntegerType, false));
        frame.printSchema();
        frame.show();
        frame.createOrReplaceTempView("test");
        Dataset<Row> result = spark.sql("select sum(age) as total, name from test group by name");
        result.printSchema();
        result.show();
        spark.close();
    }

    @Test
    public void rddTest() {
        SparkConf conf = new SparkConf().setAppName("test-rdd").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        JavaRDD<Integer> distData = sc.parallelize(data);
        Integer sum = distData.reduce((Function2<Integer, Integer, Integer>) (v1, v2) -> v1 + v2);
        System.out.println("sum value: " + sum);
        sc.close();
    }

}
