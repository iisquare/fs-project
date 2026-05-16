package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.sink.AbstractKafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class KafkaSinkNode extends AbstractKafkaSink {

    @Override
    public Object process() {
        final String topic = options.at("/topic").asText();
        String semantic = options.at("/semantic").asText();
        final String topicKey = options.at("/topicKey").asText();
        final String partitionKey = options.at("/partitionKey").asText();

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", options.at("/bootstrap").asText());

        KafkaSerializationSchema<String> schema = (KafkaSerializationSchema<String>) (element, timestamp) -> {
            String topic1 = topic;
            Integer partition = null;
            if (!DPUtil.empty(topicKey) || !DPUtil.empty(partitionKey)) {
                JsonNode json = DPUtil.parseJSON(element);
                if (!DPUtil.empty(topicKey)) topic1 = json.at("/" + topicKey).asText();
                if (!DPUtil.empty(partitionKey)) partition = json.at("/" + partitionKey).asInt();
            }
            return new ProducerRecord<>(topic1, partition, null, null, element.getBytes(StandardCharsets.UTF_8));
        };

        FlinkUtil.union(DataStream.class, sources).addSink(
                new FlinkKafkaProducer<>(topic, schema, properties, FlinkKafkaProducer.Semantic.valueOf(semantic))
        ).setParallelism(parallelism).name(getClass().getSimpleName());
        return null;
    }

}
