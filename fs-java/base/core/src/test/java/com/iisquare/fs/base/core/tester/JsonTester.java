package com.iisquare.fs.base.core.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class JsonTester {

    @Test
    public void concurrentTest() throws Exception {
        int count = 100;
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            String key = "i" + i;
            new Thread(() -> {
                ObjectNode node = DPUtil.objectNode();
                node.put(key, UUID.randomUUID().toString());
                System.out.println(node);
                latch.countDown();
            }).start();
        }
        latch.await();
    }

    @Test
    public void emptyTest() {
        ObjectNode json = DPUtil.objectNode();
        json.put("a", (String) null);
        json.putObject("b");
        json.put("d", 1);
        for (String name : Arrays.asList("a", "b", "c", "d")) {
            JsonNode node = json.at("/" + name);
            System.out.println("[name]:" + name);
            System.out.println("[null == node]:" + (null == node));
            System.out.println("[node.isNull()]:" + node.isNull());
            System.out.println("[node.isEmpty()]:" + node.isEmpty());
            System.out.println("[node.asText()]:" + node.asText());
            System.out.println("[node.asText(null)]:" + node.asText(null));
            System.out.println("[node.asInt()]:" + node.asInt());
            System.out.println("[node.asInt(0)]:" + node.asInt(0));
        }
    }

    @Test
    public void removeTest() {
        ObjectNode node = DPUtil.objectNode();
        node.put("a", 1);
        node.put("b", 2);
        node.put("c", 3);
        List<String> keys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue().asInt() == 1000) {
                keys.add(entry.getKey());
            }
        }
        node.remove(keys);
        System.out.println(node);
    }

    @Test
    public void retainTest() {
        ObjectNode nodes = DPUtil.objectNode();
        for (int i = 0; i < 5; i++) {
            ObjectNode node = nodes.putObject("i" + i);
            node.put("a", 1);
            node.put("b", 2);
            node.put("c", 3);
        }
        for (JsonNode node : nodes) {
            ((ObjectNode) node).retain("a", "c");
        }
        System.out.println(nodes);
    }

    @Test
    public void jsonTest() {
        ArrayNode nodes = DPUtil.arrayNode();
        nodes.add(1);
        nodes.add(2);
        nodes.add(3);
        printFields(nodes);
        printWhile(nodes);
        printFor(nodes);
        ObjectNode node = DPUtil.objectNode();
        node.put("a", 1);
        node.put("b", 2);
        node.put("c", 3);
        printFields(node);
        printWhile(node);
        printFor(node);
    }

    private void printFields (JsonNode json) {
        System.out.println("----------fields------------");
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            System.out.println(entry.getKey() + "->" + entry.getValue());
        }
    }

    private void printWhile (JsonNode json) {
        System.out.println("----------while------------");
        Iterator<JsonNode> iterator = json.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            System.out.println(node);
        }
    }

    private void printFor (JsonNode json) {
        System.out.println("----------for------------");
        for (JsonNode node : json) {
            System.out.println(node);
        }
    }

}
