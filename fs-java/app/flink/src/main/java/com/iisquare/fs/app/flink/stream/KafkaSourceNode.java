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
import org.apache.kafka.common.TopicPartition;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class KafkaSourceNode extends AbstractKafkaSource<DataStream<JsonNode>> {

    @Override
    public DAGType<DataStream<JsonNode>> outputType() {
        return FlinkTypes.DATASTREAM_JSON;
    }

    @Override
    public DataStream<JsonNode> process() {
        FlinkRunner runner = runner(FlinkRunner.class);
        String group = options.at("/groupId").asText();
        if (DPUtil.empty(group)) group = options.at("/group").asText();
        if (DPUtil.empty(group)) group = getClass().getSimpleName();
        String servers = options.at("/servers").asText();
        if (DPUtil.empty(servers)) servers = options.at("/bootstrap").asText();
        if (DPUtil.empty(servers)) throw new IllegalArgumentException("Kafka读取节点未配置服务地址");
        if (DPUtil.empty(options.at("/topic").asText())) {
            throw new IllegalArgumentException("Kafka读取节点未配置主题");
        }
        String startingOffsets = options.at("/startingOffsets").asText("");
        if (DPUtil.empty(startingOffsets)) startingOffsets = options.at("/offset").asText("");
        if (runner.isBatch() && (DPUtil.empty(startingOffsets) || "latest".equalsIgnoreCase(startingOffsets))) {
            startingOffsets = "earliest";
        }
        KafkaSourceBuilder<JsonNode> builder = KafkaSource.<JsonNode>builder()
                .setBootstrapServers(servers)
                .setTopics(options.at("/topic").asText())
                .setGroupId(group)
                .setStartingOffsets(offsets(startingOffsets))
                .setDeserializer(new KafkaDeserialization());
        if (runner.isBatch()) {
            String endingOffsets = options.at("/endingOffsets").asText("");
            builder.setBounded(offsets(DPUtil.empty(endingOffsets) ? "latest" : endingOffsets));
        }
        String commitInterval = options.at("/commitInterval").asText();
        if (!DPUtil.empty(commitInterval)) builder.setProperty("auto.commit.interval.ms", commitInterval);
        kafkaProperties(builder);
        String zookeeper = options.at("/zookeeper").asText();
        if (!DPUtil.empty(zookeeper)) {
            // Flink 2.x的新版Kafka连接器不再使用zookeeper配置
            System.out.println("[KafkaSourceNode] 忽略已废弃的zookeeper配置：" + zookeeper);
        }
        DataStream<JsonNode> source = runner.stream()
                .fromSource(builder.build(), WatermarkStrategy.noWatermarks(), getClass().getSimpleName())
                .setParallelism(parallelism);
        return source;
    }

    private void kafkaProperties(KafkaSourceBuilder<JsonNode> builder) {
        String securityProtocol = options.at("/securityProtocol").asText("");
        String saslMechanism = options.at("/saslMechanism").asText("");
        String saslJaasConfig = options.at("/saslJaasConfig").asText("");
        int connectTimeout = options.at("/connectTimeout").asInt(0);
        if (connectTimeout > 0) {
            builder.setProperty("socket.connection.setup.timeout.ms", String.valueOf(connectTimeout));
        }
        if (!DPUtil.empty(securityProtocol)) builder.setProperty("security.protocol", securityProtocol);
        if (!DPUtil.empty(saslMechanism)) builder.setProperty("sasl.mechanism", saslMechanism);
        if (!DPUtil.empty(saslJaasConfig)) builder.setProperty("sasl.jaas.config", saslJaasConfig);
        JsonNode properties = options.get("consumerProperties");
        if (properties != null && properties.isObject()) {
            properties.fields().forEachRemaining(entry ->
                    builder.setProperty(entry.getKey(), entry.getValue().asText()));
        }
    }

    /**
     * 起始消费位置：earliest取最早，其余（含latest/空值）取最新
     */
    public static OffsetsInitializer offsets(String offset) {
        if ("committed".equalsIgnoreCase(offset)) return OffsetsInitializer.committedOffsets();
        if (offset != null && offset.startsWith("timestamp:")) {
            return OffsetsInitializer.timestamp(DPUtil.parseLong(offset.substring("timestamp:".length())));
        }
        if (offset != null && offset.trim().startsWith("{")) {
            JsonNode json = DPUtil.parseJSON(offset);
            if (json != null && json.isObject()) {
                Map<TopicPartition, Long> result = new LinkedHashMap<>();
                Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> topic = iterator.next();
                    if (!topic.getValue().isObject()) continue;
                    topic.getValue().fields().forEachRemaining(partition -> result.put(
                            new TopicPartition(topic.getKey(), DPUtil.parseInt(partition.getKey())),
                            partition.getValue().asLong()));
                }
                if (!result.isEmpty()) return OffsetsInitializer.offsets(result);
            }
        }
        if ("earliest".equalsIgnoreCase(offset)) return OffsetsInitializer.earliest();
        return OffsetsInitializer.latest();
    }

}
