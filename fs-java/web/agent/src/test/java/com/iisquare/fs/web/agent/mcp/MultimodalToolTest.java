package com.iisquare.fs.web.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.ai.ToolDefinitions;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultimodalToolTest {

    @Test
    public void exposesImageDetailInToolSchema() throws Exception {
        JsonNode schema = DPUtil.parseJSON(ToolDefinitions.from(MultimodalTool.class.getMethod(
                "multimodalAnalyze", List.class, String.class, String.class, String.class, String.class))
                .inputSchema());

        Assert.assertTrue(schema.at("/properties/detail").isObject());
        Assert.assertEquals("string", schema.at("/properties/detail/type").asText());
        Assert.assertTrue(schema.at("/properties/detail/description").asText().contains("low"));
        Assert.assertFalse(schema.at("/required").toString().contains("\"detail\""));
    }

    @Test
    public void appliesDetailToImageUrlObject() {
        MultimodalTool tool = new MultimodalTool();

        ObjectNode part = tool.mediaPart(file("https://example.com/a.png", "image/png"), "low");

        Assert.assertEquals("image_url", part.at("/type").asText());
        Assert.assertEquals("low", part.at("/image_url/detail").asText());
    }

    @Test
    public void fileDetailOverridesTopLevelDetail() {
        MultimodalTool tool = new MultimodalTool();
        Map<String, Object> file = file("https://example.com/a.png", "image/png");
        file.put("detail", "HIGH");

        ObjectNode part = tool.mediaPart(file, "low");

        Assert.assertEquals("high", part.at("/image_url/detail").asText());
    }

    @Test
    public void doesNotSendDetailForVideoOrAudio() {
        MultimodalTool tool = new MultimodalTool();

        ObjectNode video = tool.mediaPart(file("https://example.com/a.mp4", "video/mp4"), "low");
        ObjectNode audio = tool.mediaPart(file("https://example.com/a.mp3", "audio/mpeg"), "low");

        Assert.assertFalse(video.at("/video_url").has("detail"));
        Assert.assertFalse(audio.at("/audio_url").has("detail"));
    }

    @Test
    public void rejectsUnknownDetailLevel() {
        MultimodalTool tool = new MultimodalTool();

        try {
            tool.mediaPart(file("https://example.com/a.png", "image/png"), "medium");
            Assert.fail("未知的 detail 取值应直接报错");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("low / high / original / auto"));
        }
    }

    private Map<String, Object> file(String url, String type) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("url", url);
        file.put("type", type);
        return file;
    }
}
