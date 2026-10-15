package com.iisquare.fs.app.flink;

import com.iisquare.fs.app.flink.func.ExampleProcessAllWindowFunction;
import com.iisquare.fs.app.flink.trigger.CountTimeoutTrigger;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class WindowTests {

    /**
     * Flink 2.x：新版KafkaSource替代FlinkKafkaConsumer
     */
    private static DataStreamSource<String> kafkaSource(StreamExecutionEnvironment env) {
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:9092")
                .setTopics("fs_test")
                .setGroupId("fs-test")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-source");
    }

    private static StreamExecutionEnvironment localEnv() {
        Configuration config = new Configuration();
        config.set(RestOptions.BIND_PORT, "8088");
        config.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        return StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
    }

    /**
     * 在上一次Sink完成之后，Source才会开始下一次读取
     */
    @Test
    public void kafkaTest() throws Exception {
        final StreamExecutionEnvironment env = localEnv();
        DataStreamSource<String> source = kafkaSource(env);
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
        final StreamExecutionEnvironment env = localEnv();
        DataStreamSource<String> source = kafkaSource(env);
        SingleOutputStreamOperator<String> map1 = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map1[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<List<String>> process = map1
                .windowAll(TumblingProcessingTimeWindows.of(Duration.ofSeconds(3)))
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
        final StreamExecutionEnvironment env = localEnv();
        DataStreamSource<String> source = kafkaSource(env);
        SingleOutputStreamOperator<String> map = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map[%s]:%s%n\n", new Date(), value);
            Thread.sleep(5000);
            System.out.printf("Out[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<List<String>> process = map
                .windowAll(TumblingProcessingTimeWindows.of(Duration.ofSeconds(3)))
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
        final StreamExecutionEnvironment env = localEnv();
        DataStreamSource<String> source = kafkaSource(env);
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
        final StreamExecutionEnvironment env = localEnv();
        DataStreamSource<String> source = kafkaSource(env);
        SingleOutputStreamOperator<String> map1 = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map1[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<List<String>> process = map1
                .windowAll(TumblingProcessingTimeWindows.of(Duration.ofSeconds(3)))
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
     * 无论上一次的Sink是否完成，Source会持续向缓冲区写入数据
     * Backpressure在一定程度会调节上下游处理速度，但仍然存在内存溢出的可能
     * 通过调换瓶颈Map的处理位置和并行度，合理安排开窗时机，一定程度上可以缓解上述问题
     */
    @Test
    public void backpressureTest() throws Exception {
        final StreamExecutionEnvironment env = localEnv();
        DataStreamSource<String> source = kafkaSource(env);
        SingleOutputStreamOperator<String> map1 = source.map((MapFunction<String, String>) value -> {
            System.out.printf("Map1[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<String> map2 = map1.map((MapFunction<String, String>) value -> {
            Thread.sleep(100);
            System.out.printf("Map2[%s]:%s%n\n", new Date(), value);
            return value;
        });
        SingleOutputStreamOperator<String> map3 = map2.map((MapFunction<String, String>) value -> {
            Thread.sleep(3000);
            System.out.printf("Map3[%s]:%s%n\n", new Date(), value);
            return value;
        }).setParallelism(3);
        SingleOutputStreamOperator<List<String>> process = map3
                .countWindowAll(5)
                .process(new ExampleProcessAllWindowFunction<>());
        process.addSink(new SinkFunction<>() {
            @Override
            public void invoke(List<String> value, Context context) throws Exception {
                System.out.printf("Sink[%s]:%s%n\n", new Date(), value);
                Thread.sleep(5000);
                System.out.printf("Done[%s]:%s%n\n", new Date(), value);
            }
        });
        env.execute(getClass().getSimpleName());
    }

}
