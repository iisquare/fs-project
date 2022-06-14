package com.iisquare.fs.ext.flink.job;

import com.iisquare.fs.ext.flink.util.CLIUtil;
import com.ververica.cdc.connectors.postgres.PostgreSQLSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Properties;

public class CDCJob {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(10000);
        env.setStateBackend(new HashMapStateBackend());
        CheckpointConfig checkpoint = env.getCheckpointConfig();
        checkpoint.setCheckpointStorage("file://" + CLIUtil.path() + "checkpoints");
        checkpoint.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        checkpoint.setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        PostgreSQLSource.Builder<String> builder = PostgreSQLSource.<String>builder()
                .hostname("127.0.0.1").username("postgres").password("admin888")
                .database("postgres").schemaList("public").tableList(".*")
                .decodingPluginName("pgoutput") // could not access file "decoderbufs": No such file or directory
                .deserializer(new JsonDebeziumDeserializationSchema());
        builder.debeziumProperties(new Properties(){{
            put("snapshot.mode", args[0]);
        }});
        DataStreamSource<String> source = env.addSource(builder.build());
        source.print();
        env.execute("cdc-job");
    }

}
