package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.serialization.KafkaDeserialization;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractKafkaSource;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

public class KafkaSourceNode extends AbstractKafkaSource<DataStream<JsonNode>> {

    @Override
    public DAGType<DataStream<JsonNode>> outputType() {
        return FlinkTypes.DATASTREAM_JSON;
    }

    @Override
    public DataStream<JsonNode> process() {
        String group = options.at("/group").asText();
        if (DPUtil.empty(group)) group = getClass().getSimpleName();
        KafkaSourceBuilder<JsonNode> builder = KafkaSource.<JsonNode>builder()
                .setBootstrapServers(options.at("/bootstrap").asText())
                .setTopics(options.at("/topic").asText())
                .setGroupId(group)
                .setStartingOffsets(offsets(options.at("/offset").asText()))
                .setDeserializer(new KafkaDeserialization());
        String commitInterval = options.at("/commitInterval").asText();
        if (!DPUtil.empty(commitInterval)) builder.setProperty("auto.commit.interval.ms", commitInterval);
        String zookeeper = options.at("/zookeeper").asText();
        if (!DPUtil.empty(zookeeper)) {
            // Flink 2.x的新版Kafka连接器不再使用zookeeper配置
            System.out.println("[KafkaSourceNode] 忽略已废弃的zookeeper配置：" + zookeeper);
        }
        DataStream<JsonNode> source = runner(FlinkRunner.class).stream()
                .fromSource(builder.build(), WatermarkStrategy.noWatermarks(), getClass().getSimpleName())
                .setParallelism(parallelism);
        return source;
    }

    /**
     * 起始消费位置：earliest取最早，其余（含latest/空值）取最新
     */
    public static OffsetsInitializer offsets(String offset) {
        if ("earliest".equalsIgnoreCase(offset)) return OffsetsInitializer.earliest();
        return OffsetsInitializer.latest();
    }

}
