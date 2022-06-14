package com.iisquare.fs.app.flink.tester;

import com.iisquare.fs.app.flink.output.EmptyOutput;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSourceBuilder;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.connectors.postgres.PostgreSQLSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
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
        properties.setProperty("auto.commit.enable", "true");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", "fs-test");
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> stream = env.addSource(
                new FlinkKafkaConsumer<>("fs-access-log", new SimpleStringSchema(), properties), getClass().getSimpleName());
        stream.print();
        env.execute("stream-test");
    }

    @Test
    public void sqlTest() {}

    @Test
    public void cdcTest() throws Exception {
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8080-8089");
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname("127.0.0.1").username("root").password("admin888")
                .databaseList(".*").tableList(".*")
                .startupOptions(StartupOptions.latest())
                .deserializer(new JsonDebeziumDeserializationSchema());
        DataStreamSource<String> source = env.fromSource(
                builder.build(), WatermarkStrategy.noWatermarks(), getClass().getSimpleName());
        source.print();
        env.execute("cdc-test");
    }

    @Test
    public void pgTest() throws Exception {
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8080-8089");
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        env.enableCheckpointing(1000);
        PostgreSQLSource.Builder<String> builder = PostgreSQLSource.<String>builder()
                .hostname("127.0.0.1").username("postgres").password("admin888")
                .database("postgres").schemaList("public").tableList(".*")
                .decodingPluginName("pgoutput") // could not access file "decoderbufs": No such file or directory
                .deserializer(new JsonDebeziumDeserializationSchema());
        builder.debeziumProperties(new Properties(){{
            /**
             * Postgresql的LSN保存在主数据库中，启用Checkpoint后即可confirm，且不受fromSavepoint影响
             * 清除： select * from pg_drop_replication_slot('flink');
             * 查询： select * from pg_replication_slots;
             * @see(https://ververica.github.io/flink-cdc-connectors/master/content/connectors/postgres-cdc.html)
             */
            put("snapshot.mode", "never"); // initial, always, never, initial_only, exported
        }});
        DataStreamSource<String> source = env.addSource(builder.build());
        source.print();
        env.execute("pg-test");
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

}
