package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

public class AgenticToolSchemaTest {

    @Test
    public void sanitizesNestedSchemaDescriptions() {
        ObjectNode schema = DPUtil.objectNode();
        schema.put("type", "object");
        schema.put("description", "说明\n".repeat(300));
        schema.putArray("examples").add("不该进入模型");
        schema.put("x-vendor", "remove");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("name").put("type", "string")
                .put("description", "字段说明 " + "很长".repeat(200));

        AgenticTool.sanitizeSchema(schema, 0);

        Assert.assertTrue(schema.at("/description").asText("").length() > 200);
        Assert.assertFalse(schema.has("examples"));
        Assert.assertFalse(schema.has("x-vendor"));
        Assert.assertTrue(schema.at("/properties/name/description").asText("").length() > 200);
    }
}
