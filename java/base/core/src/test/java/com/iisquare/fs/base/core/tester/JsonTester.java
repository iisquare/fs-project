package com.iisquare.fs.base.core.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonTester {

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
