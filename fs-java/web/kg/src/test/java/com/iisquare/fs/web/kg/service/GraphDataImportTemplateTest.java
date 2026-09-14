package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.kg.ontology.OntologyModel;
import com.iisquare.fs.web.kg.util.ExcelUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入模板接口：Excel 多工作表、CSV 文本、异常分支
 */
public class GraphDataImportTemplateTest {

    protected OntologyModel.Field field(String name, String title, String type, boolean required) {
        OntologyModel.Field field = new OntologyModel.Field();
        field.setName(name);
        field.setTitle(title);
        field.setType(type);
        field.setRequired(required);
        return field;
    }

    protected OntologyModel.Entity entity(String name, String label, String primaryField) {
        OntologyModel.Entity entity = new OntologyModel.Entity();
        entity.setName(name);
        entity.setLabel(label);
        entity.setPrimaryField(primaryField);
        entity.setFields(new ArrayList<>(Arrays.asList(
                field(primaryField, "编码", "String", true), field("name", "名称", "String", false))));
        return entity;
    }

    protected OntologyModel model() {
        OntologyModel.Entity company = entity("企业", "Company", "code");
        OntologyModel.Entity person = entity("人员", "Person", "code");
        OntologyModel.Relationship relationship = new OntologyModel.Relationship();
        relationship.setName("任职");
        relationship.setLabel("WORKS_FOR");
        relationship.setSourceEntity(company);
        relationship.setTargetEntity(person);
        relationship.setFields(new ArrayList<>(Arrays.asList(field("startYear", "入职年份", "Integer", false))));
        OntologyModel model = new OntologyModel();
        model.getEntities().addAll(Arrays.asList(company, person));
        model.getRelationships().add(relationship);
        return model;
    }

    protected GraphDataService service(final OntologyModel model) {
        GraphDataService service = new GraphDataService();
        service.ontologyService = new OntologyService() {
            @Override
            public OntologyModel model(int id) {
                return model;
            }

            @Override
            public com.iisquare.fs.web.kg.entity.Ontology info(Integer id) {
                return null;
            }
        };
        return service;
    }

    protected Map<String, Object> param(String entity, String relationship, String scope, String format) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ontologyId", 1);
        if (null != entity) param.put("entity", entity);
        if (null != relationship) param.put("relationship", relationship);
        if (null != scope) param.put("scope", scope);
        if (null != format) param.put("format", format);
        return param;
    }

    @Test
    public void entityTemplateIsExcelWithNoticeSheet() throws Exception {
        Map<String, Object> result = service(model()).importTemplate(param("Company", null, null, null));
        Assert.assertTrue(ApiUtil.succeed(result));
        JsonNode data = DPUtil.toJSON(result.get(ApiUtil.FIELD_DATA));
        Assert.assertEquals("xlsx", data.at("/format").asText());
        Assert.assertEquals(2, data.at("/sheets").asInt());
        Assert.assertTrue(data.at("/filename").asText().endsWith(".xlsx"));
        List<List<String>> rows = ExcelUtil.read(Base64.getDecoder().decode(data.at("/content").asText()));
        Assert.assertEquals(Arrays.asList("编码", "名称"), rows.get(0));
        Assert.assertEquals("ID001", rows.get(1).get(0));
    }

    @Test
    public void allScopeGeneratesEntityAndRelationshipSheets() {
        Map<String, Object> result = service(model()).importTemplate(param(null, null, "all", null));
        Assert.assertTrue(ApiUtil.succeed(result));
        JsonNode data = DPUtil.toJSON(result.get(ApiUtil.FIELD_DATA));
        Assert.assertEquals(4, data.at("/sheets").asInt()); // 2个实体 + 1个关系 + 字段说明
        Assert.assertTrue(data.at("/filename").asText().contains("图数据导入模板"));
    }

    @Test
    public void csvScopeReturnsPlainText() {
        Map<String, Object> result = service(model()).importTemplate(param(null, "WORKS_FOR", null, "csv"));
        Assert.assertTrue(ApiUtil.succeed(result));
        JsonNode data = DPUtil.toJSON(result.get(ApiUtil.FIELD_DATA));
        Assert.assertEquals("csv", data.at("/format").asText());
        Assert.assertTrue(data.at("/filename").asText().endsWith(".csv"));
        Assert.assertTrue(data.at("/content").asText().startsWith("source,target,入职年份"));
    }

    @Test
    public void csvRejectsAllScope() {
        Map<String, Object> result = service(model()).importTemplate(param(null, null, "all", "csv"));
        Assert.assertFalse(ApiUtil.succeed(result));
        Assert.assertEquals("CSV不支持多工作表，请改为Excel模板或选择单个实体/关系", ApiUtil.message(result));
    }

    @Test
    public void unknownStructureReturnsError() {
        Map<String, Object> result = service(model()).importTemplate(param("NotExist", null, null, null));
        Assert.assertFalse(ApiUtil.succeed(result));
        Assert.assertEquals("实体定义不存在", ApiUtil.message(result));
    }

}
