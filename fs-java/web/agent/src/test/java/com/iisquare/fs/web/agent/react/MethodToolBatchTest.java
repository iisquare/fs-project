package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.entity.Tool;
import com.iisquare.fs.web.agent.entity.ToolMethod;
import com.iisquare.fs.web.agent.service.ToolMethodService;
import com.iisquare.fs.web.agent.service.ToolService;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodToolBatchTest {

    @Test
    public void methodsFromSameToolLoadToolsAndMethodsOnce() {
        ToolService toolService = mock(ToolService.class);
        ToolMethodService methodService = mock(ToolMethodService.class);
        MethodTool tool = new MethodTool();
        tool.toolService = toolService;
        tool.toolMethodService = methodService;
        when(toolService.infos(any())).thenReturn(List.of(
                Tool.builder().id(7).name("订单工具").status(1).build()));
        when(methodService.all(org.mockito.ArgumentMatchers.<Collection<Integer>>any())).thenReturn(List.of(
                method(7, "getOrder", "查询订单"),
                method(7, "createOrder", "创建订单")));
        when(methodService.methodParams(any())).thenReturn(List.of(
                Map.of("name", "id", "required", true,
                        "schema", Map.of("type", "string"))));

        ArrayNode items = DPUtil.arrayNode();
        items.add(item("getOrder"));
        items.add(item("createOrder"));

        ArrayNode definitions = tool.definitions(items);

        Assert.assertEquals(2, definitions.size());
        Assert.assertEquals("getOrder", definitions.at("/0/function/name").asText());
        Assert.assertEquals("createOrder", definitions.at("/1/function/name").asText());
        verify(toolService, times(1)).infos(any());
        verify(methodService, times(1)).all(org.mockito.ArgumentMatchers.<Collection<Integer>>any());
        verify(methodService, never()).all(any(Integer.class));
    }

    private ObjectNode item(String name) {
        ObjectNode item = DPUtil.objectNode();
        item.put("kind", "method");
        item.put("toolId", 7);
        item.put("method", name);
        item.put("name", name);
        return item;
    }

    private ToolMethod method(int toolId, String name, String title) {
        return ToolMethod.builder()
                .toolId(toolId)
                .name(name)
                .title(title)
                .description(title)
                .params("[]")
                .status(1)
                .present(1)
                .build();
    }
}
