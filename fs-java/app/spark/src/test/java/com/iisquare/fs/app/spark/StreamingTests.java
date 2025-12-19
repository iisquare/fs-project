package com.iisquare.fs.app.spark;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.junit.Test;
import scala.Tuple2;

import java.util.*;

public class StreamingTests {

    @Test
    public void windowTest() throws InterruptedException {
        SparkConf sparkConf = new SparkConf().setAppName("test-window").setMaster("local[*]");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(5));
        // jssc.checkpoint("hdfs://path/to/checkpoint/dir");
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", "kafka:9092");
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", "fs-test");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);

        Collection<String> topics = List.of("fs_test");
        JavaInputDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(topics, kafkaParams)
        );
        JavaDStream<String> lines = stream.map((Function<ConsumerRecord<String, String>, String>) record -> {
            System.out.printf("map:%s%n\n", record);
            return record.value();
        });
        JavaDStream<String> words = lines.flatMap((FlatMapFunction<String, String>) record -> {
            Thread.sleep(100);
            System.out.printf("flatMap:%s%n\n", record);
            return Arrays.asList(StringUtils.SPACE.split(record)).iterator();
        });
        JavaPairDStream<String, Integer> wordCounts = words.repartition(3).mapToPair((PairFunction<String, String, Integer>) record -> {
            Thread.sleep(3000);
            System.out.printf("mapToPair:%s%n\n", record);
            return Tuple2.apply(record, 1);
        });
        JavaPairDStream<String, Integer> windowedWordCounts = wordCounts.window(Durations.seconds(5));
        windowedWordCounts.print();
        jssc.start();
        jssc.awaitTermination();
    }

}
