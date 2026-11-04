package com.iisquare.fs.web.oa.service;

import com.iisquare.fs.web.oa.entity.FormFrame;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class PrintRenderServiceTests {

    @Test
    public void renderWithFriendlyFieldNames() throws Exception {
        FormFrame frame = FormFrame.builder().content("""
                {"widgets":[
                  {"type":"text","label":"申请人","options":{"field":"widget-applicant"}},
                  {"type":"subform","label":"采购明细","options":{
                    "field":"widget-items",
                    "formInfo":{"widgets":[
                      {"type":"text","label":"物品名称","options":{"field":"widget-name"}},
                      {"type":"number","label":"数量","options":{"field":"widget-count"}}
                    ]}
                  }}
                ]}
                """).build();
        PrintRenderService service = new PrintRenderService();
        List<Map<String, Object>> fields = service.fields(frame);
        assertTrue(fields.stream().anyMatch(item ->
                "申请人".equals(item.get("field")) && "widget-applicant".equals(item.get("id"))));
        Map<String, Object> subform = fields.stream()
                .filter(item -> "采购明细".equals(item.get("field")))
                .findFirst().orElseThrow();
        assertTrue("widget-items".equals(subform.get("id")));
        assertTrue("子表单".equals(subform.get("component")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) subform.get("children");
        assertTrue(children.stream().anyMatch(item ->
                "物品名称".equals(item.get("field"))
                        && "采购明细.物品名称".equals(item.get("path"))
                        && "单行文本".equals(item.get("component"))));

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("widget-applicant", "张三");
        model.put("widget-items", List.of(
                Map.of("widget-name", "铅笔", "widget-count", 2)
        ));
        model = service.aliasFieldData(model, fields);

        byte[] template;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("申请人：{{申请人}}，复核：{{申请人}}");
            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("{{采购明细}}");
            table.getRow(1).getCell(0).setText("[物品名称]");
            table.getRow(1).getCell(1).setText("[数量]");
            document.write(output);
            template = output.toByteArray();
        }

        List<Map<String, Object>> placeholders = service.placeholders(template);
        assertTrue(placeholders.stream().anyMatch(item ->
                "{{申请人}}".equals(item.get("raw")) && Integer.valueOf(2).equals(item.get("count"))));
        assertTrue(placeholders.stream().anyMatch(item ->
                "[物品名称]".equals(item.get("raw")) && "row".equals(item.get("type"))));

        byte[] result = service.render(template, model, service.listFields(fields));
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result))) {
            String paragraphText = String.join("\n", document.getParagraphs().stream()
                    .map(XWPFParagraph::getText).toList());
            assertTrue(paragraphText.contains("申请人：张三"));
            String tableText = document.getTables().get(0).getText();
            assertTrue(tableText, tableText.contains("铅笔"));
            assertTrue(tableText, tableText.contains("2"));
        }
    }

    @Test
    public void renderSingleValueAndParagraphList() throws Exception {
        byte[] template;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Heading1");
            title.createRun().setText("采购申请单");
            XWPFParagraph customer = document.createParagraph();
            customer.createRun().setText("客户：{{customer.na");
            customer.createRun().setText("me}}，编号：{{widget-1789094011326490153}}");
            document.createParagraph().createRun().setText("{{?items}}");
            document.createParagraph().createRun().setText("项目：{{name}}，金额：{{amount}}");
            document.createParagraph().createRun().setText("{{/items}}");
            document.write(output);
            template = output.toByteArray();
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("customer", Map.of("name", "张三"));
        model.put("widget-1789094011326490153", "A-001");
        model.put("items", Arrays.asList(
                Map.of("name", "设计费", "amount", 100),
                Map.of("name", "实施费", "amount", 200)
        ));
        PrintRenderService service = new PrintRenderService();
        byte[] result = service.render(template, model, List.of());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result))) {
            String text = String.join("\n", document.getParagraphs().stream().map(XWPFParagraph::getText).toList());
            assertTrue(text.contains("客户：张三"));
            assertTrue(text.contains("编号：A-001"));
            assertTrue(text.contains("项目：设计费，金额：100"));
            assertTrue(text.contains("项目：实施费，金额：200"));
        }
        assertTrue(service.placeholders(template).stream().anyMatch(item -> "customer.name".equals(item.get("name"))));
        assertTrue(service.placeholders(template).stream().anyMatch(item -> "list".equals(item.get("type"))));
        assertTrue(service.outline(result).stream().anyMatch(item -> "采购申请单".equals(item.get("name"))));
    }

    @Test
    public void renderTableRowList() throws Exception {
        byte[] template;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(2, 3);
            table.getRow(0).getCell(0).setText("{{items}}");
            table.getRow(1).getCell(0).setText("[widget-name]");
            table.getRow(1).getCell(1).setText("[amount]");
            document.write(output);
            template = output.toByteArray();
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("items", Arrays.asList(
                Map.of("widget-name", "设计费", "amount", 100),
                Map.of("widget-name", "实施费", "amount", 200)
        ));
        PrintRenderService service = new PrintRenderService();
        byte[] result = service.render(template, model, List.of("items"));

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result))) {
            XWPFTable table = document.getTables().get(0);
            String text = table.getText();
            assertTrue(text.contains("设计费"));
            assertTrue(text.contains("200"));
            assertTrue(text.contains("2"));
        }
    }

    @Test
    public void renderSingleBraceAndReportUnresolved() throws Exception {
        byte[] template;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("姓名：{customer.na");
            paragraph.createRun().setText("me}，缺失：{missing}");
            document.write(output);
            template = output.toByteArray();
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("customer", Map.of("name", "李四"));
        PrintRenderService service = new PrintRenderService();
        byte[] result = service.render(template, model, List.of());
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result))) {
            String text = document.getParagraphs().get(0).getText();
            assertTrue(text.contains("姓名：李四"));
        }
        assertTrue(service.unresolved(template, model).contains("missing"));
    }

}
