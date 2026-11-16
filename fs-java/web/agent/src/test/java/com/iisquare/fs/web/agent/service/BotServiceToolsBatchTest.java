package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.react.AgenticToolService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BotServiceToolsBatchTest {

    @Test
    public void allToolDefinitionsAreAssembledInOneBatch() {
        BotService service = new BotService();
        AgenticToolService definitionService = mock(AgenticToolService.class);
        AgenticService agenticService = mock(AgenticService.class);
        service.agenticToolService = definitionService;
        service.agenticService = agenticService;
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(agenticService.authorizedByIds(any(), eq(request))).thenReturn(Collections.emptyMap());
        when(definitionService.definitionSlots(any(JsonNode.class))).thenReturn(slots());
        ObjectNode body = DPUtil.objectNode();
        ArrayNode tools = body.putArray("tools");
        tools.add(methodRef("getOrder"));
        tools.add(methodRef("createOrder"));

        ObjectNode data = ApiUtil.data(service.tools(body.toString(), request), ObjectNode.class);

        Assert.assertEquals(2, data.at("/rows").size());
        Assert.assertEquals("getOrder", data.at("/rows/0/function/name").asText());
        Assert.assertEquals("createOrder", data.at("/rows/1/function/name").asText());
        Assert.assertEquals("getOrder", data.at("/rows/0/ref/functionName").asText());
        Assert.assertEquals("createOrder", data.at("/rows/1/ref/functionName").asText());
        verify(definitionService, times(1)).definitionSlots(any(JsonNode.class));
    }

    private ObjectNode methodRef(String name) {
        ObjectNode ref = DPUtil.objectNode();
        ref.put("kind", "method");
        ref.put("toolId", 7);
        ref.put("methodName", name);
        ref.put("title", name);
        return ref;
    }

    private ArrayNode slots() {
        ArrayNode slots = DPUtil.arrayNode();
        slots.add(slot(0, "getOrder"));
        slots.add(slot(1, "createOrder"));
        return slots;
    }

    private ObjectNode slot(int index, String name) {
        ObjectNode slot = DPUtil.objectNode();
        slot.put("index", index);
        slot.put("functionName", name);
        slot.set("definition", AgenticToolDefinition.definition(name));
        return slot;
    }

    private static class AgenticToolDefinition {
        private static ObjectNode definition(String name) {
            ObjectNode function = DPUtil.objectNode();
            function.put("name", name);
            function.put("description", name);
            function.putObject("parameters").put("type", "object").putObject("properties");
            ObjectNode definition = DPUtil.objectNode();
            definition.put("type", "function");
            definition.set("function", function);
            return definition;
        }
    }
}
