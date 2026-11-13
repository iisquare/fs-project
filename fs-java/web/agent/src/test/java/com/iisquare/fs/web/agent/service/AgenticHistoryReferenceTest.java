package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

public class AgenticHistoryReferenceTest {

    @Test
    public void storesRenderedUserMessagePerLlmNode() {
        ObjectNode result = DPUtil.objectNode();
        ArrayNode steps = result.putArray("steps");
        ObjectNode step = steps.addObject();
        step.put("id", "llm-1");
        step.put("type", "LLM");
        step.put("status", 1);
        ObjectNode request = step.putObject("request");
        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("role", "system").put("content", "system");
        messages.addObject().put("role", "user").put("content", "上一轮");
        messages.addObject().put("role", "assistant").put("content", "上一轮回复");
        messages.addObject().put("role", "user")
                .put("content", "这是什么\n[{\"id\":\"file-1\",\"name\":\"a.png\"}]");

        String reference = new AgenticService().historyReference(result);
        JsonNode metadata = DPUtil.parseJSON(reference);

        Assert.assertNotNull(metadata);
        Assert.assertEquals("这是什么\n[{\"id\":\"file-1\",\"name\":\"a.png\"}]",
                metadata.at("/userMessages/llm-1").asText(""));
    }
}
