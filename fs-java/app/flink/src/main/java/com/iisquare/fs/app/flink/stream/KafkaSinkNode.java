package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.serialization.KafkaMessageMapFunction;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractKafkaSink;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;

public class KafkaSinkNode extends AbstractKafkaSink<Void> {

    @Override
    public DAGType<Void> outputType() {
        return FlinkTypes.VOID;
    }

    @Override
    public Void process() {
        final String topic = options.at("/topic").asText();
        final String topicKey = options.at("/topicKey").asText();
        final String partitionKey = options.at("/partitionKey").asText();

        KafkaRecordSerializationSchema<String> schema = (element, context, timestamp) -> {
            String target = topic;
            Integer partition = null;
            if (!DPUtil.empty(topicKey) || !DPUtil.empty(partitionKey)) {
                JsonNode json = DPUtil.parseJSON(element);
                if (null != json) {
                    if (!DPUtil.empty(topicKey)) target = json.at("/" + topicKey).asText(topic);
                    if (!DPUtil.empty(partitionKey)) partition = json.at("/" + partitionKey).asInt();
                }
            }
            return new ProducerRecord<>(target, partition, null, null, element.getBytes(StandardCharsets.UTF_8));
        };

        DeliveryGuarantee guarantee = guarantee(options.at("/semantic").asText());
        String servers = options.at("/servers").asText();
        if (DPUtil.empty(servers)) servers = options.at("/bootstrap").asText();
        if (DPUtil.empty(servers)) throw new IllegalArgumentException("Kafka写入节点未配置服务地址");
        if (DPUtil.empty(topic) && DPUtil.empty(topicKey)) {
            throw new IllegalArgumentException("Kafka写入节点未配置主题");
        }
        KafkaSinkBuilder<String> builder = KafkaSink.<String>builder()
                .setBootstrapServers(servers)
                .setRecordSerializer(schema)
                .setDeliveryGuarantee(guarantee);
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
        JsonNode properties = options.get("producerProperties");
        if (properties != null && properties.isObject()) {
            properties.fields().forEachRemaining(entry ->
                    builder.setProperty(entry.getKey(), entry.getValue().asText()));
        }
        if (DeliveryGuarantee.EXACTLY_ONCE.equals(guarantee)) {
            // Exactly-Once 需要事务前缀，且依赖检查点
            String suffix = DPUtil.empty(name) ? "sink" : name.replaceAll("[^A-Za-z0-9._-]", "_");
            builder.setTransactionalIdPrefix("fs-kafka-" + suffix);
        }
        DataStream<Object> stream = (DataStream<Object>) (DataStream<?>) FlinkUtil.union(inputs());
        if (stream == null) return null;
        DataStream<String> messages = stream
                .map(new KafkaMessageMapFunction(options.at("/messageField").asText("message")))
                .returns(Types.STRING);
        messages.sinkTo(builder.build())
                .setParallelism(parallelism).name(getClass().getSimpleName());
        return null;
    }

    /**
     * 投递语义：NONE / AT_LEAST_ONCE / EXACTLY_ONCE，非法或缺省时回退为AT_LEAST_ONCE
     */
    public static DeliveryGuarantee guarantee(String semantic) {
        if (DPUtil.empty(semantic)) return DeliveryGuarantee.AT_LEAST_ONCE;
        try {
            return DeliveryGuarantee.valueOf(semantic.trim().toUpperCase());
        } catch (Exception e) {
            return DeliveryGuarantee.AT_LEAST_ONCE;
        }
    }

}
