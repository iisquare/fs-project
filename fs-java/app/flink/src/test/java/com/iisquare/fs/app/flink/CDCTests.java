package com.iisquare.fs.app.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cdc.connectors.mongodb.MongoDBSource;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSource;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSourceBuilder;
import org.apache.flink.cdc.connectors.mysql.table.StartupOptions;
import org.apache.flink.cdc.connectors.postgres.PostgreSQLSource;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class CDCTests {

    @Test
    public void mysqlTest() throws Exception {
        Configuration config = new Configuration();
        config.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        config.set(RestOptions.BIND_PORT, "8080-8089");
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        env.enableCheckpointing(30000);
        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname("127.0.0.1").username("root").password("admin888")
                .databaseList(".*").tableList(".*")
                .startupOptions(StartupOptions.latest())
                .serverTimeZone("Asia/Shanghai")
                .deserializer(new JsonDebeziumDeserializationSchema());
        DataStreamSource<String> source = env.fromSource(
                builder.build(), WatermarkStrategy.noWatermarks(), getClass().getSimpleName());
        source.print();

        KafkaRecordSerializationSchema<String> schema = (element, context, timestamp) -> new ProducerRecord<>(
                "fs_cdc_test", null, null, null, element.getBytes(StandardCharsets.UTF_8));
        KafkaSinkBuilder<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("kafka:9092")
                .setRecordSerializer(schema)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE);
        source.sinkTo(sink.build());
        env.execute("cdc-test");
    }

    @Test
    public void postgresTest() throws Exception {
        Configuration config = new Configuration();
        config.set(RestOptions.BIND_PORT, "8080-8089");
        config.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        env.enableCheckpointing(1000);
        PostgreSQLSource.Builder<String> builder = PostgreSQLSource.<String>builder()
                .hostname("127.0.0.1").username("postgres").password("admin888")
                .database("postgres").schemaList("public").tableList(".*")
                .decodingPluginName("pgoutput") // could not access file "decoderbufs": No such file or directory
                .deserializer(new JsonDebeziumDeserializationSchema(true));
        builder.debeziumProperties(new Properties(){{
            /**
             * Postgresql的LSN保存在主数据库中，启用Checkpoint后即可confirm，且不受fromSavepoint影响
             * 清除： select * from pg_drop_replication_slot('flink');
             * 查询： select * from pg_replication_slots;
             * select pg_walfile_name_offset(flush_lsn), * from pg_stat_replication;
             * select pg_walfile_name_offset(confirmed_flush_lsn), * from pg_replication_slots;
             * select pg_walfile_name_offset(pg_current_wal_flush_lsn()), pg_current_wal_flush_lsn();
             * @see(https://ververica.github.io/flink-cdc-connectors/master/content/connectors/postgres-cdc.html)
             */
            put("snapshot.mode", "never"); // initial, always, never, initial_only, exported
        }});
        DataStreamSource<String> source = env.addSource(builder.build());
        source.print();
        env.execute("pg-test");
    }

    @Test
    public void mongoTest() throws Exception {
        Configuration config = new Configuration();
        config.set(RestOptions.BIND_PORT, "8080-8089");
        config.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        env.enableCheckpointing(1000); // 重启时需要手动加载检查点文件
        MongoDBSource.Builder<String> builder = MongoDBSource.<String>builder()
                .hosts("node101:27017") // 支持分片集群，指定为mongos查询路由服务
                .username("root")
                .password("admin888")
                .databaseList("fs_test")
                .collectionList(".*")
                .deserializer(new JsonDebeziumDeserializationSchema());
        DataStreamSource<String> source = env.addSource(builder.build());
        source.print();
        env.execute("cdc-mongo");
    }

}
