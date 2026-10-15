package com.iisquare.fs.app.flink.job;

import com.iisquare.fs.app.flink.util.CLIUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSource;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSourceBuilder;
import org.apache.flink.cdc.connectors.mysql.table.StartupOptions;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class MysqlCDCJob {
    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration();
        // Flink 2.x：状态后端与检查点存储改为通过配置项指定（原 setStateBackend / setCheckpointStorage 已移除）
        configuration.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "file://" + CLIUtil.path() + "checkpoints");
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);
        env.enableCheckpointing(10000);
        CheckpointConfig checkpoint = env.getCheckpointConfig();
        checkpoint.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        // Flink 2.x：ExternalizedCheckpointCleanup 已由 ExternalizedCheckpointRetention 取代
        checkpoint.setExternalizedCheckpointRetention(ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);
        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname("127.0.0.1").username("root").password("admin888")
                .databaseList(".*").tableList(".*")
                .startupOptions(StartupOptions.latest())
                .deserializer(new JsonDebeziumDeserializationSchema());
        DataStreamSource<String> source = env.fromSource(
                builder.build(), WatermarkStrategy.noWatermarks(), MysqlCDCJob.class.getSimpleName());
        source.print();
        env.execute("mysql-cdc-job");
    }
}
