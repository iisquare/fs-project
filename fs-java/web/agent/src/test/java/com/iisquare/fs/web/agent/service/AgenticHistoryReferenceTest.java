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

    @Test
    public void storesInputFilesForDialogHistory() {
        ObjectNode result = DPUtil.objectNode();
        ObjectNode inputs = DPUtil.objectNode();
        ArrayNode files = inputs.putArray("files");
        ObjectNode file = files.addObject();
        file.put("id", "file-1");
        file.put("name", "需求说明.pdf");
        file.put("type", "application/pdf");
        file.put("suffix", "pdf");
        file.put("size", 2048);
        ObjectNode image = files.addObject();
        image.put("id", "file-2");
        image.put("name", "界面截图.png");
        image.put("type", "image/png");
        image.put("suffix", "png");
        image.put("size", 4096);

        String reference = new AgenticService().userReference(result, inputs);
        JsonNode metadata = DPUtil.parseJSON(reference);

        Assert.assertNotNull(metadata);
        Assert.assertEquals(2, metadata.at("/files").size());
        Assert.assertEquals("file-1", metadata.at("/files/0/id").asText(""));
        Assert.assertEquals("需求说明.pdf", metadata.at("/files/0/name").asText(""));
        Assert.assertEquals("pdf", metadata.at("/files/0/suffix").asText(""));
        Assert.assertEquals(2048, metadata.at("/files/0/size").asInt());
        Assert.assertEquals("file-2", metadata.at("/files/1/id").asText(""));
        Assert.assertEquals("界面截图.png", metadata.at("/files/1/name").asText(""));
    }

    @Test
    public void exposesInputFilesInRunLogs() {
        ObjectNode inputs = DPUtil.objectNode();
        ObjectNode file = inputs.putArray("files").addObject();
        file.put("id", "file-1");
        file.put("name", "需求说明.pdf");
        ArrayNode rows = DPUtil.arrayNode();
        rows.addObject().put("inputs", DPUtil.stringify(inputs));

        AgenticService service = new AgenticService();
        JsonNode list = service.formatLog(rows.deepCopy(), false);
        JsonNode detail = service.formatLog(rows.deepCopy(), true);

        Assert.assertEquals(1, list.at("/0/attachmentCount").asInt());
        Assert.assertEquals("file-1", detail.at("/0/files/0/id").asText(""));
        Assert.assertEquals("需求说明.pdf", detail.at("/0/files/0/name").asText(""));
    }
}
