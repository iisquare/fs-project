package com.iisquare.fs.app.flink.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.func.ExampleBroadcastProcessFunction;
import com.iisquare.fs.app.flink.func.ExampleProcessAllWindowFunction;
import com.iisquare.fs.app.flink.func.ExampleRandomSourceFunction;
import com.iisquare.fs.app.flink.output.EmptyOutput;
import com.iisquare.fs.app.flink.trigger.CountTimeoutTrigger;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FlinkTester {

    @Test
    public void batchTest() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        DataSource<Integer> source = env.fromCollection(data);
        source.print();
        source.output(new EmptyOutput<>());
        env.execute("batch-test");
    }

    @Test
    public void streamTest() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("zookeeper.connect", "zookeeper:2181/kafka");
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("auto.commit.enable", "false");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>("fs-access-log", new SimpleStringSchema(), properties);
        consumer.setCommitOffsetsOnCheckpoints(true);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> stream = env.addSource(consumer, getClass().getSimpleName());
        stream.print();
        env.execute("stream-test");
    }

    @Test
    public void sqlTest() throws Exception {}

    @Test
    public void restTest() throws Exception {
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8081");
        config.setInteger("taskmanager.numberOfTaskSlots", 3);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        // nc -l 8888
        DataStreamSource<String> source = env.socketTextStream("127.0.0.1", 8888);
        source.print().setParallelism(1);
        env.execute("rest-test");
    }

    @Test
    public void windowTest() throws Exception {
        Configuration config = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        DataStreamSource<String> source = env.addSource(new ExampleRandomSourceFunction());
        SingleOutputStreamOperator<List<String>> process = source
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(3)))
                .trigger(new CountTimeoutTrigger<>(5)).process(new ExampleProcessAllWindowFunction<>());
        process.print();
        env.execute("window-test");
    }

    @Test
    public void broadcastTest() throws Exception {
        Configuration config = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        DataStreamSource<ObjectNode> source = env.fromCollection(Arrays.asList(
                DPUtil.objectNode().put("id", 1).put( "type", 1).put("name", "a"),
                DPUtil.objectNode().put("id", 2).put( "type", 2).put("name", "b"),
                DPUtil.objectNode().put("id", 3).put( "type", 1).put("name", "c")
        ));
        DataStreamSource<Map<Integer, String>> dict = env.fromCollection(Arrays.asList(
                DPUtil.buildMap(Integer.class, String.class, 1, "Y", 2, "N"),
                DPUtil.buildMap(Integer.class, String.class, 1, "是", 2, "否")
        ));
        BroadcastStream<Map<Integer, String>> broadcast = dict.broadcast(ExampleBroadcastProcessFunction.descriptor);
        SingleOutputStreamOperator<JsonNode> dataset = source.connect(broadcast).process(new ExampleBroadcastProcessFunction());
        dataset.print();
        env.execute("broadcast-test");
    }

}
