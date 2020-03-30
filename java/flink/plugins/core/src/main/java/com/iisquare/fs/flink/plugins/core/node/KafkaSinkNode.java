package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class KafkaSinkNode extends Node {

    String topic;
    Properties properties;
    String field;

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        properties = new Properties();
        properties.setProperty("bootstrap.servers", config.get("bootstrap").asText());
        properties.setProperty("message.send.max.retries", "3");
        topic = config.get("topic").asText();
        field = config.get("field").asText();
        return config;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                for (Node node : current.getSource()) {
                    node.getStream().result().addSink(new FlinkKafkaProducer<Map<String, Object>>(
                        topic, new KafkaSerializationSchema(field), properties,Optional.of( new KafkaPollingPartitioner()))).name(current.getClass().getSimpleName());
                }
                return null;
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                for (Node node : current.getSource()) {
                    node.getStream().result().addSink(new FlinkKafkaProducer<Map<String, Object>>(
                        topic, new KafkaSerializationSchema(field), properties, Optional.of(new KafkaPollingPartitioner()))).name(current.getClass().getSimpleName());
                }
                return null;
            }
        };
    }
}

class KafkaPollingPartitioner extends FlinkKafkaPartitioner<Map<String, Object>> {

    private int index = 0;

    @Override
    public int partition(Map<String, Object> record, byte[] key, byte[] value, String targetTopic, int[] partitions) {
        index = index < partitions.length ? index : 0;
        return partitions[index++];
    }
}

class KafkaSerializationSchema implements SerializationSchema<Map<String, Object>> {

    private String field;

    public KafkaSerializationSchema(String field) {
        this.field = field;
    }

    @Override
    public byte[] serialize(Map<String, Object> element) {
        String message = DPUtil.empty(field) ? DPUtil.stringify(element) : DPUtil.parseString(element.get(field));
        return message.getBytes();
    }
}
