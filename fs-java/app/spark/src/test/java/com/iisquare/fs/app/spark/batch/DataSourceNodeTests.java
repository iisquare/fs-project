package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.stream.KafkaSourceNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataSourceNodeTests {

    @Test
    public void cypherLimitTest() {
        assertEquals("MATCH (n) RETURN n LIMIT 20",
                Neo4jSourceNode.limitedCypher("MATCH (n) RETURN n;", 20));
        assertEquals("MATCH (n) RETURN n LIMIT 10",
                Neo4jSourceNode.limitedCypher("MATCH (n) RETURN n LIMIT 500", 10));
        assertEquals("MATCH (n) RETURN n LIMIT 5",
                Neo4jSourceNode.limitedCypher("MATCH (n) RETURN n LIMIT 5", 20));
        assertEquals("MATCH (n) RETURN n LIMIT $limit",
                Neo4jSourceNode.limitedCypher("MATCH (n) RETURN n LIMIT $limit", 20));
    }

    @Test
    public void effectiveLimitTest() {
        assertEquals(20, ElasticsearchSourceNode.effectiveLimit(0, 20));
        assertEquals(10, ElasticsearchSourceNode.effectiveLimit(10, 20));
        assertEquals(10, ElasticsearchSourceNode.effectiveLimit(100, 10));
        assertEquals(0, ElasticsearchSourceNode.effectiveLimit(0, 0));
    }

    @Test
    public void kafkaServersTest() {
        KafkaSourceNode node = new KafkaSourceNode();
        node.setOptions(DPUtil.parseJSON("{\"bootstrap\":\"legacy:9092\"}"));
        assertEquals("legacy:9092", node.servers());
        node.setOptions(DPUtil.parseJSON("{\"servers\":\"modern:9092\",\"bootstrap\":\"legacy:9092\"}"));
        assertEquals("modern:9092", node.servers());
        node.setOptions(DPUtil.parseJSON("{\"offset\":\"earliest\",\"endingOffsets\":\"100\"}"));
        assertEquals("earliest", node.startingOffsets(true));
        assertEquals("100", node.endingOffsets());
        node.setOptions(DPUtil.parseJSON("{\"offset\":\"latest\"}"));
        assertEquals("earliest", node.startingOffsets(false));
        assertEquals("latest", node.startingOffsets(true));
    }
}
