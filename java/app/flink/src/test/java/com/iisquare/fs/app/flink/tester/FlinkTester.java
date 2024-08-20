package com.iisquare.fs.app.flink.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.func.ExampleBroadcastProcessFunction;
import com.iisquare.fs.app.flink.func.ExampleProcessAllWindowFunction;
import com.iisquare.fs.app.flink.func.ExampleRandomSourceFunction;
import com.iisquare.fs.app.flink.output.EmptyOutput;
import com.iisquare.fs.app.flink.trigger.CountTimeoutTrigger;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.TypeUtil;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

public class FlinkTester implements Serializable {

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
    public void noTest() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> source = env.fromCollection(Arrays.asList("a", "b", "c"));
        List<String> list = source.executeAndCollect(2);
        System.out.println(list);
        env.close();
    }

    @Test
    public void sqlTest() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        final StreamTableEnvironment environment = StreamTableEnvironment.create(env);
        DataStream<Row> stream = env.fromCollection(Arrays.asList(
                FlinkUtil.row(1, "a", 0.5),
                FlinkUtil.row(2, "b", 0.5),
                FlinkUtil.row(3, "c", 0.7)
        ), FlinkUtil.type(new LinkedHashMap<String, String>() {{
            put("id", Integer.class.getName());
            put("name", String.class.getName());
            put("score", Double.class.getName());
        }}));
        environment.createTemporaryView("test", stream);
        String sql = "select * from test where id > 1";
        Table table = environment.sqlQuery(sql);
        DataStream<Row> ds = environment.toDataStream(table);
        List<Row> rows = ds.executeAndCollect(1);
        System.out.println(rows);
        environment.dropTemporaryView("test");
        env.close();
    }

    @Test
    public void m1Test() throws Exception {
        final StreamExecutionEnvironment env1 = StreamExecutionEnvironment.createLocalEnvironment();
        final StreamExecutionEnvironment env2 = StreamExecutionEnvironment.createLocalEnvironment();
        FileUtil.close(env1, env2);
    }

    @Test
    public void m2Test() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        final StreamTableEnvironment env1 = StreamTableEnvironment.create(env);
        final StreamTableEnvironment env2 = StreamTableEnvironment.create(env);
        DataStream<Row> stream = env.fromCollection(Arrays.asList(
                FlinkUtil.row(1, "a", 0.5),
                FlinkUtil.row(2, "b", 0.5),
                FlinkUtil.row(3, "c", 0.7)
        ), FlinkUtil.type(new LinkedHashMap<String, String>() {{
            put("id", Integer.class.getName());
            put("name", String.class.getName());
            put("score", Double.class.getName());
        }}));
        env1.createTemporaryView("test", stream);
        CloseableIterator<Row> iterator = env2.sqlQuery("select * from test").execute().collect();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            System.out.println(row);
        }
        FileUtil.close(env);
    }

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
    public void remoteTest() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createRemoteEnvironment(
                "127.0.0.1", 8081
        );
        DataStream<Row> stream = env.fromCollection(Arrays.asList(
                FlinkUtil.row(1, "a", 0.5),
                FlinkUtil.row(2, "b", 0.5),
                FlinkUtil.row(3, "c", 0.7)
        ), FlinkUtil.type(new LinkedHashMap<String, String>() {{
            put("id", Integer.class.getName());
            put("name", String.class.getName());
            put("score", Double.class.getName());
        }}));
        CloseableIterator<Row> iterator = stream.map((MapFunction<Row, Row> & Serializable) row -> {
            row.setField(2, TypeUtil._double(row.getFieldAs(2)) * 10);
            return row;
        }).executeAndCollect("remote-test");
        while (iterator.hasNext()) {
            Row next = iterator.next();
            System.out.println(next);
        }
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

    @Test
    public void typeTest() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<Row> stream = env.fromCollection(Arrays.asList(
                FlinkUtil.row(1, "a", 0.5),
                FlinkUtil.row(2, "b", 0.5),
                FlinkUtil.row(3, "c", 0.7)
        ), FlinkUtil.type(new LinkedHashMap<String, String>() {{
            put("id", Integer.class.getName());
            put("name", String.class.getName());
            put("score", Double.class.getName());
        }}));
        stream = stream.map((MapFunction<Row, Row>) row -> {
            double score = row.getFieldAs("score");
            return FlinkUtil.row(row.getField("id"), row.getField("name"), score * 100);
        }).returns(stream.getType());
        System.out.println(stream.getType());
        stream.print();
        env.execute();
    }

}
