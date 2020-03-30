package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchema;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.flink.api.java.typeutils.TypeExtractor.getForClass;

public class KafkaSourceNode extends Node {
    String topic;
    Properties properties;
    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        properties = new Properties();
        properties.setProperty("bootstrap.servers", config.get("bootstrap").asText());
        properties.setProperty("zookeeper.connect", config.get("zookeeper").asText());
        properties.setProperty("auto.offset.reset", config.get("offset").asText());
        properties.setProperty("auto.commit.enable", "true");
//        properties.setProperty("auto.commit.interval.ms", "1000");
        properties.setProperty("group.id", config.get("group").asText());
        topic = config.get("topic").asText();
        setTypeInfo(new RowTypeInfo(new TypeInformation[]{
            Types.STRING, Types.INT, Types.LONG, Types.STRING
        }, new String[]{"topic", "partition", "offset", "message"}));
        return config;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                return environment().addSource(
                    new FlinkKafkaConsumer<>(topic, new KafkaDeserializationSchema(), properties), current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return null;
    }
}

class KafkaDeserializationSchema implements KeyedDeserializationSchema<Map<String, Object>> {

    @Override
    public TypeInformation getProducedType() {
        return getForClass(Map.class);
    }

    @Override
    public Map<String, Object> deserialize(byte[] messageKey, byte[] message, String topic, int partition, long offset) throws IOException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("topic", topic);
        row.put("partition", partition);
        row.put("offset", offset);
        row.put("message", new String(message));
        return row;
    }

    @Override
    public boolean isEndOfStream(Map<String, Object> nextElement) {
        return false;
    }

}
