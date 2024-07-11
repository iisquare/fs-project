package com.iisquare.fs.app.flink.tester;

import com.iisquare.fs.app.flink.func.ExampleProcessAllWindowFunction;
import com.iisquare.fs.app.flink.trigger.CountTimeoutTrigger;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class WindowTester {

    /**
     * 在上一次Sink完成之后，Source才会开始下一次读取
     */
    @Test
    public void kafkaTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("zookeeper.connect", "zookeeper:2181/kafka");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("auto.commit.enable", "false");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>("fs_test", new SimpleStringSchema(), properties);
        consumer.setCommitOffsetsOnCheckpoints(true);
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8088");
        config.setInteger(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        DataStreamSource<String> source = env.addSource(consumer);
        SingleOutputStreamOperator<String> map = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map[%s]:%s%n\n", new Date(), value);
            return value;
        });
        map.addSink(new SinkFunction<String>() {
            @Override
            public void invoke(String value, Context context) throws Exception {
                System.out.printf("Sink[%s]:%s%n\n", new Date(), value);
                Thread.sleep(5000);
                System.out.printf("Done[%s]:%s%n\n", new Date(), value);
            }
        });
        env.execute(getClass().getSimpleName());
    }

    /**
     * 由于时间窗口的存在，在上一次Sink未完成之前，Source会持续读取，导致内存异常
     */
    @Test
    public void windowTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("zookeeper.connect", "zookeeper:2181/kafka");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("auto.commit.enable", "false");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>("fs_test", new SimpleStringSchema(), properties);
        consumer.setCommitOffsetsOnCheckpoints(true);
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8088");
        config.setInteger(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        DataStreamSource<String> source = env.addSource(consumer);
        SingleOutputStreamOperator<String> map1 = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map1[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<List<String>> process = map1
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(3)))
                .trigger(new CountTimeoutTrigger<>(5)).process(new ExampleProcessAllWindowFunction<>());
        System.out.println(process.getType());
        SingleOutputStreamOperator<List<String>> map2 = process.map((MapFunction<List<String>, List<String>>) value -> {
            System.out.printf("Map2[%s]:%s%n\n", new Date(), Arrays.toString(value.toArray(new String[0])));
            return value;
        }).returns(process.getType());
        map2.addSink(new SinkFunction<List<String>>() {
            @Override
            public void invoke(List<String> value, Context context) throws Exception {
                System.out.printf("Sink[%s]:%s%n\n", new Date(), Arrays.toString(value.toArray(new String[0])));
                Thread.sleep(10000);
                System.out.printf("Done[%s]:%s%n\n", new Date(), Arrays.toString(value.toArray(new String[0])));
            }
        });
        env.execute(getClass().getSimpleName());
    }

    /**
     * 读取延迟大于窗口时间周期时，时间窗口依然会在延迟设定时间后输出，即Sink延迟=Source读取延迟+时间窗口延迟
     */
    @Test
    public void timeoutTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("zookeeper.connect", "zookeeper:2181/kafka");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("auto.commit.enable", "false");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>("fs_test", new SimpleStringSchema(), properties);
        consumer.setCommitOffsetsOnCheckpoints(true);
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8088");
        config.setInteger(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        DataStreamSource<String> source = env.addSource(consumer);
        SingleOutputStreamOperator<String> map = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map[%s]:%s%n\n", new Date(), value);
            Thread.sleep(5000);
            System.out.printf("Out[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<List<String>> process = map
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(3)))
                .process(new ExampleProcessAllWindowFunction<>());
        process.addSink(new SinkFunction<List<String>>() {
            @Override
            public void invoke(List<String> value, Context context) throws Exception {
                System.out.printf("Sink[%s]:%s%n\n", new Date(), value);
            }
        });
        env.execute(getClass().getSimpleName());
    }

    /**
     * 无论上一次的Sink是否完成，countWindowAll依然会持续读取数据源
     */
    @Test
    public void countTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("zookeeper.connect", "zookeeper:2181/kafka");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("auto.commit.enable", "false");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>("fs_test", new SimpleStringSchema(), properties);
        consumer.setCommitOffsetsOnCheckpoints(true);
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8088");
        config.setInteger(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        DataStreamSource<String> source = env.addSource(consumer);
        SingleOutputStreamOperator<String> map = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<List<String>> process = map
                .countWindowAll(5)
                .process(new ExampleProcessAllWindowFunction<>());
        process.addSink(new SinkFunction<List<String>>() {
            @Override
            public void invoke(List<String> value, Context context) throws Exception {
                System.out.printf("Sink[%s]:%s%n\n", new Date(), value);
                Thread.sleep(5000);
                System.out.printf("Done[%s]:%s%n\n", new Date(), value);
            }
        });
        env.execute(getClass().getSimpleName());
    }

    @Test
    public void fixedTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("zookeeper.connect", "zookeeper:2181/kafka");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("auto.commit.enable", "false");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>("fs_test", new SimpleStringSchema(), properties);
        consumer.setCommitOffsetsOnCheckpoints(true);
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8088");
        config.setInteger(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        DataStreamSource<String> source = env.addSource(consumer);
        SingleOutputStreamOperator<String> map1 = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map1[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<List<String>> process = map1
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(3)))
                .trigger(new CountTimeoutTrigger<>(5)).process(new ExampleProcessAllWindowFunction<>());
        System.out.println(process.getType());
        SingleOutputStreamOperator<List<String>> map2 = process.map((MapFunction<List<String>, List<String>>) value -> {
            System.out.printf("Map2[%s]:%s%n\n", new Date(), Arrays.toString(value.toArray(new String[0])));
            return value;
        }).returns(process.getType());
        map2.addSink(new SinkFunction<List<String>>() {
            @Override
            public void invoke(List<String> value, Context context) throws Exception {
                System.out.printf("Sink[%s]:%s%n\n", new Date(), Arrays.toString(value.toArray(new String[0])));
                Thread.sleep(10000);
                System.out.printf("Done[%s]:%s%n\n", new Date(), Arrays.toString(value.toArray(new String[0])));
            }
        });
        env.execute(getClass().getSimpleName());
    }

}
