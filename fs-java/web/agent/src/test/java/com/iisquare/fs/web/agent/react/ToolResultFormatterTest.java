package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

public class ToolResultFormatterTest {

    @Test
    public void fullFormatKeepsOriginalResult() {
        ObjectNode value = DPUtil.objectNode();
        value.put("themeId", 1);
        Assert.assertSame(value, ToolResultFormatter.format("theme", value, "full"));
    }

    @Test
    public void themeQueryUsesColumnarJsonWithoutJdbcMetadata() {
        ObjectNode source = DPUtil.objectNode();
        source.put("themeId", 9);
        source.put("themeName", "销售主题");
        source.put("description", "销售订单分析主题");
        source.put("sql", "select id, amount from orders");
        ObjectNode sourceDataset = source.putArray("datasets").addObject();
        sourceDataset.put("id", 7);
        sourceDataset.put("name", "orders");
        sourceDataset.put("description", "订单主表");
        sourceDataset.putArray("columns").addObject().put("name", "id")
                .put("title", "订单ID").put("type", "bigint").put("comment", "订单主键");
        source.putArray("relations").addObject().put("sourceDatasetId", 7).put("targetDatasetId", 8);
        ObjectNode query = source.putObject("data");
        query.put("limit", 100);
        query.put("timeout", 15);
        ArrayNode columns = query.putArray("columns");
        columns.addObject().put("index", 1).put("name", "id").put("label", "订单ID")
                .put("type", "bigint")
                .put("jdbcType", -5).put("className", "java.lang.Long");
        columns.addObject().put("index", 2).put("name", "amount").put("label", "金额")
                .put("type", "decimal").put("precision", 18).put("scale", 2);
        ArrayNode rows = query.putArray("rows");
        rows.addObject().put("id", 1001).put("amount", 12.5);

        JsonNode result = (JsonNode) ToolResultFormatter.format("theme", source, "brief");

        Assert.assertFalse(result.has("themeId"));
        Assert.assertEquals("销售主题", result.at("/themeName").asText());
        Assert.assertEquals("销售订单分析主题", result.at("/description").asText());
        Assert.assertEquals("订单主表", result.at("/datasets/0/description").asText());
        Assert.assertEquals("订单主键", result.at("/datasets/0/columns/0/comment").asText());
        Assert.assertEquals(7, result.at("/relations/0/sourceDatasetId").asInt());
        Assert.assertEquals("id", result.at("/columns/0").asText());
        Assert.assertEquals("amount", result.at("/columns/1").asText());
        Assert.assertEquals("订单ID", result.at("/columnMeta/0/label").asText());
        Assert.assertEquals("bigint", result.at("/columnMeta/0/type").asText());
        Assert.assertEquals(18, result.at("/columnMeta/1/precision").asInt());
        Assert.assertFalse(result.at("/columnMeta/0").has("jdbcType"));
        Assert.assertEquals(1001, result.at("/rows/0/0").asInt());
        Assert.assertEquals(12.5, result.at("/rows/0/1").asDouble(), 0.001);
    }

    @Test
    public void knowledgeReturnsHitsWithoutDuplicatingText() {
        ObjectNode source = DPUtil.objectNode();
        source.put("knowledgeId", 3);
        source.put("knowledgeName", "产品手册");
        ObjectNode document = source.putArray("documents").addObject();
        document.put("id", "doc-1");
        document.put("name", "安装说明.pdf");
        ObjectNode row = source.putArray("result").addObject();
        row.put("documentId", "doc-1");
        row.put("content", "先关闭电源，再安装。");
        row.put("_score", 0.92);
        source.put("text", "先关闭电源，再安装。");

        JsonNode result = (JsonNode) ToolResultFormatter.format("knowledge", source, "brief");

        Assert.assertFalse(result.has("knowledgeId"));
        Assert.assertFalse(result.has("text"));
        Assert.assertEquals("doc-1", result.at("/documents/0/id").asText());
        Assert.assertEquals("安装说明.pdf", result.at("/documents/0/name").asText());
        Assert.assertEquals("doc-1", result.at("/hits/0/documentId").asText());
        Assert.assertEquals("安装说明.pdf", result.at("/hits/0/documentName").asText());
        Assert.assertEquals("先关闭电源，再安装。", result.at("/hits/0/content").asText());
        Assert.assertEquals(0.92, result.at("/hits/0/score").asDouble(), 0.001);
    }

    @Test
    public void themeDictionaryKeepsDatasetIdsUsedByRelations() {
        ObjectNode source = DPUtil.objectNode();
        source.put("themeName", "销售主题");
        ObjectNode dataset = source.putArray("datasets").addObject();
        dataset.put("id", 7);
        dataset.put("name", "orders");
        dataset.put("description", "订单明细表");
        dataset.putArray("columns").addObject()
                .put("name", "id").put("title", "id").put("type", "bigint").put("comment", "订单主键");
        ObjectNode relation = source.putArray("relations").addObject();
        relation.put("sourceDatasetId", 7);
        relation.put("targetDatasetId", 8);

        JsonNode result = (JsonNode) ToolResultFormatter.format("theme", source, "brief");

        Assert.assertEquals(7, result.at("/datasets/0/id").asInt());
        Assert.assertEquals("orders", result.at("/datasets/0/name").asText());
        Assert.assertEquals("订单明细表", result.at("/datasets/0/description").asText());
        Assert.assertEquals("id", result.at("/datasets/0/columns/0/title").asText());
        Assert.assertEquals("订单主键", result.at("/datasets/0/columns/0/comment").asText());
        Assert.assertEquals(7, result.at("/relations/0/sourceDatasetId").asInt());
    }

    @Test
    public void methodObjectArrayUsesColumnsOnce() {
        ArrayNode source = DPUtil.arrayNode();
        source.addObject().put("id", 1).put("name", "A");
        source.addObject().put("id", 2).put("name", "B");

        JsonNode result = (JsonNode) ToolResultFormatter.format("method", source, "brief");

        Assert.assertEquals("id", result.at("/columns/0").asText());
        Assert.assertEquals("name", result.at("/columns/1").asText());
        Assert.assertEquals(2, result.at("/rows/1/0").asInt());
        Assert.assertEquals("B", result.at("/rows/1/1").asText());
    }

    @Test
    public void methodCallDropsRequestEchoAndCompactsJsonResponse() {
        ObjectNode source = DPUtil.objectNode();
        source.put("success", true);
        source.put("status", 200);
        source.putObject("request")
                .put("url", "https://example.test/orders")
                .putObject("body").put("page", 1);
        source.putObject("header").put("Content-Type", "application/json");
        source.put("response", "{\"data\":{\"rows\":[{\"id\":1,\"name\":\"A\"},"
                + "{\"id\":2,\"name\":\"B\"}],\"total\":2}}");

        JsonNode result = (JsonNode) ToolResultFormatter.format("method", source, "brief");

        Assert.assertTrue(result.at("/success").asBoolean());
        Assert.assertEquals(200, result.at("/status").asInt());
        Assert.assertFalse(result.has("request"));
        Assert.assertFalse(result.has("header"));
        Assert.assertEquals("id", result.at("/response/data/columns/0").asText());
        Assert.assertEquals(2, result.at("/response/data/rows/1/0").asInt());
    }

    @Test
    public void methodBusinessResultKeepsOwnSuccessField() {
        ObjectNode source = DPUtil.objectNode();
        source.put("success", true);
        source.putObject("data").put("answer", "已处理");

        JsonNode result = (JsonNode) ToolResultFormatter.format("method", source, "brief");

        Assert.assertTrue(result.at("/success").asBoolean());
        Assert.assertEquals("已处理", result.at("/data/answer").asText());
    }

    @Test
    public void methodPlainTextResponseKeepsOriginalType() {
        ObjectNode source = DPUtil.objectNode();
        source.put("success", true);
        source.putObject("request").put("url", "https://example.test");
        source.put("response", "001");

        JsonNode result = (JsonNode) ToolResultFormatter.format("method", source, "brief");

        Assert.assertTrue(result.at("/response").isTextual());
        Assert.assertEquals("001", result.at("/response").asText());
    }

    @Test
    public void ontologyPathKeepsReadablePathOnly() {
        ObjectNode source = DPUtil.objectNode();
        source.put("action", "path");
        source.put("truncated", true);
        source.putObject("from").put("entity", "Order").put("id", "ORD-1001");
        source.putObject("to").put("entity", "Customer").put("id", "CUST-1");
        source.putArray("text").add("订单1001 -[属于]-> 客户A");
        source.putArray("paths").addObject();
        source.putArray("nodes").addObject();
        source.putArray("relationships").addObject();

        JsonNode result = (JsonNode) ToolResultFormatter.format("ontology", source, "brief");

        Assert.assertTrue(result.at("/truncated").asBoolean());
        Assert.assertEquals("ORD-1001", result.at("/from/id").asText());
        Assert.assertEquals("CUST-1", result.at("/to/id").asText());
        Assert.assertEquals("订单1001 -[属于]-> 客户A", result.at("/text/0").asText());
        Assert.assertFalse(result.has("paths"));
        Assert.assertFalse(result.has("nodes"));
        Assert.assertFalse(result.has("relationships"));
    }

    @Test
    public void ontologySearchUsesColumnarRows() {
        ObjectNode source = DPUtil.objectNode();
        source.put("ontologyId", 2);
        source.put("entity", "Order");
        source.put("keyword", "1001");
        source.put("total", 1);
        ObjectNode row = source.putArray("rows").addObject();
        row.put("elementId", "neo-1");
        row.putArray("labels").add("Order");
        row.putObject("properties").put("id", "ORD-1001").put("amount", 99.5);

        JsonNode result = (JsonNode) ToolResultFormatter.format("ontology", source, "brief");

        Assert.assertFalse(result.has("ontologyId"));
        Assert.assertEquals("elementId", result.at("/columns/0").asText());
        Assert.assertEquals("labels", result.at("/columns/1").asText());
        Assert.assertEquals("neo-1", result.at("/rows/0/0").asText());
        Assert.assertEquals("Order", result.at("/rows/0/1/0").asText());
        JsonNode thirdValue = result.get("rows").get(0).get(2);
        Assert.assertEquals(result.toString(),
                "{\"id\":\"ORD-1001\",\"amount\":99.5}", thirdValue.toString());
        Assert.assertEquals("ORD-1001", thirdValue.get("id").asText());
        Assert.assertEquals(99.5, thirdValue.get("amount").asDouble(), 0.001);
    }

    @Test
    public void agenticSuccessKeepsAnswerOnly() {
        ObjectNode source = DPUtil.objectNode();
        source.put("answer", "任务已完成");
        source.put("status", "success");
        source.put("error", "");

        JsonNode result = (JsonNode) ToolResultFormatter.format("agentic", source, "brief");

        Assert.assertEquals("任务已完成", result.at("/answer").asText());
        Assert.assertFalse(result.has("status"));
        Assert.assertFalse(result.has("error"));
    }
}
