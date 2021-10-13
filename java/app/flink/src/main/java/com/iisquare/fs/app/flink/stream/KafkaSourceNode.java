package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.serialization.KafkaDeserialization;
import com.iisquare.fs.base.dag.source.AbstractKafkaSource;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

public class KafkaSourceNode extends AbstractKafkaSource {
    @Override
    public Object process() {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", options.at("/bootstrap").asText());
        properties.setProperty("zookeeper.connect", options.at("/zookeeper").asText());
        properties.setProperty("auto.offset.reset", options.at("/offset").asText());
        properties.setProperty("auto.commit.enable", "true");
        properties.setProperty("auto.commit.interval.ms", options.at("/commitInterval").asText());
        properties.setProperty("group.id", options.at("/group").asText());
        DataStreamSource<JsonNode> source = runner(FlinkRunner.class).stream().addSource(new FlinkKafkaConsumer<>(
                options.at("/topic").asText(), new KafkaDeserialization(), properties), getClass().getSimpleName());
        return source;
    }
}
