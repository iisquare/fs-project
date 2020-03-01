package com.iisquare.fs.rest.flink.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import java.util.*;

public class DPTester {

    @Test
    public void parseTest() {
        System.out.println("1:" + DPUtil.parseBoolean(1));
        System.out.println("0:" + DPUtil.parseBoolean(0));
        System.out.println("true:" + DPUtil.parseBoolean(true));
        System.out.println("false:" + DPUtil.parseBoolean(false));
        System.out.println("1:" + DPUtil.parseBoolean("1"));
        System.out.println("0:" + DPUtil.parseBoolean("0"));
        System.out.println("true:" + DPUtil.parseBoolean("true"));
        System.out.println("false:" + DPUtil.parseBoolean("false"));
    }

    @Test
    public void replaceTest() {
        System.out.println("saf/aclasssf/asf/asf.class".replaceFirst("\\.class", "").replaceAll("/", "."));
    }

    private ObjectNode extend(ObjectNode node, Object ext) {
        if(null == ext) return node;
        String json = DPUtil.stringify(ext);
        if(null == json) return node;
        JsonNode data = DPUtil.parseJSON(json);
        Iterator<String> iterator = data.fieldNames();
        while (iterator.hasNext()) {
            String field = iterator.next();
            node.replace(field, data.get(field));
        }
        return node;
    }

    @Test
    public void extentTest() {
        ObjectNode node = DPUtil.objectNode();
        node.put("a", 1);
        node.put("b", 2);
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("a", "casfafs");
        ext.put("c", Arrays.asList(1, 2, 3, 4, 5));
        System.out.println(DPUtil.stringify(extend(node, ext)));
    }

}
