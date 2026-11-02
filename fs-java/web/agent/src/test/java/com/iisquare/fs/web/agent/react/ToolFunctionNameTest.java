package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ToolFunctionNameTest {

    @Test
    public void normalizesIllegalCharactersAndCollisions() {
        ObjectNode first = DPUtil.objectNode();
        first.put("kind", "knowledge");
        first.put("name", "设备 手册");
        first.put("knowledgeId", 12);
        ObjectNode second = DPUtil.objectNode();
        second.put("kind", "theme");
        second.put("name", "设备 手册");
        second.put("themeId", 13);

        Set<String> used = new HashSet<>();
        Assert.assertEquals("knowledge_12", ToolFunctionName.unique(first, used));
        Assert.assertEquals("theme_13", ToolFunctionName.unique(second, used));

        first.put("name", "get weather");
        second.put("name", "get weather");
        used.clear();
        Assert.assertEquals("get_weather", ToolFunctionName.unique(first, used));
        Assert.assertEquals("get_weather_2", ToolFunctionName.unique(second, used));
    }

    @Test
    public void methodUsesMethodName() {
        ObjectNode item = DPUtil.objectNode();
        item.put("kind", "method");
        item.put("method", "get-weather");
        item.put("toolId", 1);
        Assert.assertEquals("get-weather", ToolFunctionName.unique(item, new HashSet<>()));
    }
}
