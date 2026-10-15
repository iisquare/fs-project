package com.iisquare.fs.app.flink.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.charset.StandardCharsets;

/**
 * Kafka 记录反序列化：将消息封装为 {topic, partition, offset, message} 结构
 */
public class KafkaDeserialization implements KafkaRecordDeserializationSchema<JsonNode> {

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<JsonNode> out) {
        ObjectNode row = DPUtil.objectNode();
        row.put("topic", record.topic());
        row.put("partition", record.partition());
        row.put("offset", record.offset());
        row.put("message", null == record.value() ? "" : new String(record.value(), StandardCharsets.UTF_8));
        out.collect(row);
    }

    @Override
    public TypeInformation<JsonNode> getProducedType() {
        return TypeInformation.of(JsonNode.class);
    }
}
