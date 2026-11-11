package com.iisquare.fs.app.flink;

import com.iisquare.fs.app.flink.serialization.KafkaMessageMapFunction;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class KafkaNodeTests {

    @Test
    public void messageFieldTest() {
        KafkaMessageMapFunction function = new KafkaMessageMapFunction("message");
        assertEquals("{\"id\":1}",
                function.map(DPUtil.parseJSON("{\"topic\":\"t\",\"message\":\"{\\\"id\\\":1}\"}")));
        assertEquals("{\"topic\":\"t\",\"offset\":3}",
                function.map(DPUtil.parseJSON("{\"topic\":\"t\",\"offset\":3}")));
        assertEquals("plain", function.map("plain"));
    }

    @Test
    public void offsetsRangeTest() {
        OffsetsInitializer initializer = com.iisquare.fs.app.flink.stream.KafkaSourceNode.offsets(
                "{\"orders\":{\"0\":100,\"1\":200}}");
        Map<TopicPartition, Long> offsets = initializer.getPartitionOffsets(
                java.util.List.of(new TopicPartition("orders", 0), new TopicPartition("orders", 1)), null);
        assertEquals(Long.valueOf(100L), offsets.get(new TopicPartition("orders", 0)));
        assertEquals(Long.valueOf(200L), offsets.get(new TopicPartition("orders", 1)));
    }
}
