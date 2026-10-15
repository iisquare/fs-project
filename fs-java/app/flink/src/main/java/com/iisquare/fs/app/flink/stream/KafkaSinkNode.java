package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractKafkaSink;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
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
        KafkaSinkBuilder<String> builder = KafkaSink.<String>builder()
                .setBootstrapServers(options.at("/bootstrap").asText())
                .setRecordSerializer(schema)
                .setDeliveryGuarantee(guarantee);
        if (DeliveryGuarantee.EXACTLY_ONCE.equals(guarantee)) {
            // Exactly-Once 需要事务前缀，且依赖检查点
            builder.setTransactionalIdPrefix(getClass().getSimpleName() + "-" + name);
        }
        FlinkUtil.<String>union(inputs()).sinkTo(builder.build())
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
