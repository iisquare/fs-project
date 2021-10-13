package com.iisquare.fs.app.spark.tester;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SparkTester {

    @Test
    public void batchTest() {
        SparkConf conf = new SparkConf().setAppName("batch-test").setMaster("local");
        JavaSparkContext context = new JavaSparkContext(conf);
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        JavaRDD<Integer> rdd = context.parallelize(data);
        rdd.foreach(item -> {
            System.out.println(item);
        });
        context.close();
    }

    /**
     * 创建临时Socket服务
     * nc -lk 9999
     */
    @Test
    public void streamTest() throws Exception {
        SparkSession session = SparkSession.builder().appName("stream-test").master("local").getOrCreate();
        Dataset<Row> lines = session.readStream().format("socket")
                .option("host", "localhost").option("port", 9999).load();
        Dataset<String> words = lines.as(Encoders.STRING())
                .flatMap((FlatMapFunction<String, String>) x -> Arrays.asList(x.split(" ")).iterator(), Encoders.STRING());
        Dataset<Row> wordCounts = words.groupBy("value").count();
        try {
            StreamingQuery query = wordCounts.writeStream().outputMode("complete").format("console").start();
            query.awaitTermination();
        } finally {
            session.close();
        }
    }

    @Test
    public void sqlTest() {

    }

}
