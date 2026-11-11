package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class AIServiceModelsTest {

    @Test
    public void authorizedModelsExposeOnlyGatewayModelFields() {
        StubAIService service = new StubAIService();
        ObjectNode payload = DPUtil.objectNode();
        ArrayNode data = payload.putArray("data");
        data.addObject()
                .put("id", "qwen-alias")
                .put("owned_by", "fs-gateway")
                .put("context_length", 32768)
                .putObject("provider")
                .put("token", "must-not-leak");
        data.addObject()
                .put("id", "deepseek-alias")
                .put("owned_by", "fs-gateway")
                .put("max_position_embeddings", 65536);
        service.response = ApiUtil.result(0, null, payload);

        Map<String, Object> result = service.authorizedModels("QWEN");

        Assert.assertFalse(ApiUtil.failed(result));
        JsonNode rows = ApiUtil.data(result, ObjectNode.class).at("/rows");
        Assert.assertEquals(1, rows.size());
        JsonNode row = rows.get(0);
        Assert.assertEquals("qwen-alias", row.at("/name").asText());
        Assert.assertEquals(32768, row.at("/contextLength").asInt());
        Assert.assertFalse(row.has("provider"));
        Assert.assertFalse(DPUtil.stringify(row).contains("must-not-leak"));
    }

    private static class StubAIService extends AIService {
        private Map<String, Object> response;

        @Override
        public Map<String, Object> models() {
            return response;
        }
    }
}
