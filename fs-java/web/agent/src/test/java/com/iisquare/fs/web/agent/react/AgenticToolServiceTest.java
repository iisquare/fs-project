package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.service.ToolDescriptionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AgenticToolServiceTest {

    private AgenticToolService service;

    @Before
    public void setUp() {
        service = new AgenticToolService();
        service.toolDescriptionService = new ToolDescriptionService();
        service.toolList = List.of(new FakeTool());
        service.registry();
    }

    @Test
    public void definitionAndInvocationUseSameCollisionSuffix() {
        ArrayNode items = DPUtil.arrayNode();
        items.add(item("same", true));
        items.add(item("same", true));

        ArrayNode definitions = service.definitions(items);
        Assert.assertEquals(2, definitions.size());
        Assert.assertEquals("same", definitions.at("/0/function/name").asText());
        Assert.assertEquals("same_2", definitions.at("/1/function/name").asText());
        Assert.assertEquals("second", service.invoke(
                null, items, "same_2", Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void unavailableDefinitionStillReservesItsName() {
        ArrayNode items = DPUtil.arrayNode();
        items.add(item("same", false));
        items.add(item("same", true));

        ArrayNode definitions = service.definitions(items);
        Assert.assertEquals(1, definitions.size());
        Assert.assertEquals("same_2", definitions.at("/0/function/name").asText());
        Assert.assertEquals("second", service.invoke(
                null, items, "same_2", Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void briefFormatIsAppliedAfterInvocation() {
        ArrayNode items = DPUtil.arrayNode();
        items.add(item("rows", true));

        Object value = service.invoke(null, items, "rows", Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), "brief");
        JsonNode result = DPUtil.toJSON(value);

        Assert.assertEquals("id", result.at("/columns/0").asText());
        Assert.assertEquals(1, result.at("/rows/0/0").asInt());
    }

    protected ObjectNode item(String name, boolean available) {
        ObjectNode item = DPUtil.objectNode();
        item.put("kind", "fake");
        item.put("name", name);
        item.put("available", available);
        return item;
    }

    protected static class FakeTool implements AgenticTool {

        @Override
        public String kind() {
            return "fake";
        }

        @Override
        public ObjectNode definition(JsonNode item) {
            if (!item.at("/available").asBoolean(false)) return null;
            return AgenticTool.definition(item.at("/name").asText(""), DPUtil.arrayNode(), item);
        }

        @Override
        public Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                             Map<String, ObjectNode> outputs, Map<String, Object> variables) {
            if ("rows".equals(item.at("/name").asText())) {
                ArrayNode rows = DPUtil.arrayNode();
                rows.addObject().put("id", 1);
                return rows;
            }
            return "same".equals(item.at("/name").asText()) ? "second" : "first";
        }
    }
}
