package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

public class ToolDescriptionServiceTest {

    private final ToolDescriptionService service = new ToolDescriptionService();

    @Test
    public void knowledgeUsesTargetNameWithoutDetail() {
        ObjectNode item = DPUtil.objectNode();
        item.put("kind", "knowledge");
        item.put("name", "knowledge_search_12");
        item.put("knowledgeName", "设备维护手册");
        item.put("description", "查询故障代码、维修步骤和设备参数");

        ObjectNode tool = DPUtil.objectNode();
        tool.put("type", "function");
        tool.putObject("function").put("name", "knowledge_search_12");
        String description = service.apply(tool, item).at("/function/description").asText("");

        Assert.assertTrue(description.contains("设备维护手册"));
        Assert.assertFalse(description.contains("knowledge_search_12"));
        Assert.assertFalse(description.contains("故障代码"));
    }

    @Test
    public void multiActionDescriptionExplainsCapabilities() {
        String description = service.description("ontology", "供应链图谱", "");
        Assert.assertTrue(description.startsWith("查询知识图谱「供应链图谱」"));
        Assert.assertTrue(description.contains("检索实体"));
        Assert.assertTrue(description.contains("关联路径"));
    }

    @Test
    public void longDetailIsNotTruncated() {
        String detail = "说明".repeat(300);
        String description = service.description("agentic", "退款处理", detail);
        Assert.assertTrue(description.contains(detail));
        Assert.assertFalse(description.endsWith("…"));
    }

    @Test
    public void methodDescriptionKeepsMultilineContent() {
        String description = service.description("method", "天气 / 查询天气",
                "按城市\n和日期查询天气");
        Assert.assertEquals("工具方法「天气 / 查询天气」：按城市\n和日期查询天气。", description);
    }

    @Test
    public void lookupToolsUseFixedTemplatesOnly() {
        String knowledge = apply("knowledge", "knowledge_1", "description", "知识库详情");
        String theme = apply("theme", "theme_1", "description", "数据主题详情");
        String ontology = apply("ontology", "ontology_1", "description", "知识图谱详情");
        Assert.assertFalse(knowledge.contains("知识库详情"));
        Assert.assertFalse(theme.contains("数据主题详情"));
        Assert.assertFalse(ontology.contains("知识图谱详情"));
    }

    protected String apply(String kind, String name, String field, String detail) {
        ObjectNode item = DPUtil.objectNode();
        item.put("kind", kind);
        item.put("name", name);
        item.put(field, detail);
        ObjectNode tool = DPUtil.objectNode();
        tool.put("type", "function");
        tool.putObject("function").put("name", name);
        return service.apply(tool, item).at("/function/description").asText("");
    }
}
