package com.iisquare.fs.base.core.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataTester {

    @Test
    public void numberTest() {
        // java.lang.NumberFormatException: For input string
        System.out.println(Integer.valueOf(""));
    }

    @Test
    public void arrayTest() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] strings = list.toArray(new String[0]);
        System.out.println(Arrays.toString(strings));
        System.out.println(Arrays.toString(DPUtil.array(String.class, 2)));
    }

    @Test
    public void jsonTest() {
        ObjectNode json = DPUtil.objectNode();
        ArrayNode array = json.putObject("a").putArray("b");
        array.addObject().put("c", 1);
        array.addObject().put("d", "xxx");
        array.addObject().putObject("e").put("f", 3);
        System.out.println(DPUtil.value(json, "a.b.2.e.f", DPUtil.toJSON(666)));
        JsonNode result = DPUtil.value(json, "a.b.2.e");
        System.out.println(DPUtil.stringify(result));
    }

}
