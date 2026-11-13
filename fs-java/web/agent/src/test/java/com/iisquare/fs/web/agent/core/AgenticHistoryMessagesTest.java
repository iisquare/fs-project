package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

public class AgenticHistoryMessagesTest {

    @Test
    public void restoresRenderedUserMessageForCurrentNode() {
        ObjectNode data = DPUtil.objectNode();
        ObjectNode memory = data.putObject("memory");
        memory.put("enabled", true);
        memory.put("scope", "conversation");
        memory.put("window", 10);
        memory.put("toolchain", false);

        ArrayNode history = DPUtil.arrayNode();
        ObjectNode question = history.addObject();
        question.put("role", "user");
        question.put("content", "这是什么");
        question.putObject("nodeContents")
                .put("llm-1", "这是什么\n[{\"id\":\"file-1\",\"name\":\"a.png\"}]");
        history.addObject().put("role", "assistant").put("content", "上一轮回复");

        AgenticRuntime runtime = new AgenticRuntime();
        ArrayNode messages = runtime.historyMessages(history, data, "llm-1");
        Assert.assertEquals("这是什么\n[{\"id\":\"file-1\",\"name\":\"a.png\"}]",
                messages.at("/0/content").asText(""));

        ArrayNode fallback = runtime.historyMessages(history, data, "other-node");
        Assert.assertEquals("这是什么", fallback.at("/0/content").asText(""));
    }
}
