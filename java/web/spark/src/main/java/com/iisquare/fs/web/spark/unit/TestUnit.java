package com.iisquare.fs.web.spark.unit;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.SparkSession;

import java.util.Arrays;
import java.util.List;

public class TestUnit {

    public static List<Double> random(SparkSession session) {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        JavaSparkContext context = new JavaSparkContext(session.sparkContext());
        JavaRDD<Integer> rdd = context.parallelize(data);
        JavaRDD<Double> result = rdd.map((Function<Integer, Double>) value -> value / Math.random());
        return result.collect();
    }

}
