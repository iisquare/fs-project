package com.iisquare.fs.app.flink.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaDeserialization implements KafkaDeserializationSchema<JsonNode> {

    @Override
    public boolean isEndOfStream(JsonNode nextElement) {
        return false;
    }

    @Override
    public JsonNode deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
        ObjectNode row = DPUtil.objectNode();
        row.put("topic", record.topic());
        row.put("partition", record.partition());
        row.put("offset", record.offset());
        row.put("message", new String(record.value()));
        return row;
    }

    @Override
    public TypeInformation<JsonNode> getProducedType() {
        return TypeInformation.of(JsonNode.class);
    }
}
