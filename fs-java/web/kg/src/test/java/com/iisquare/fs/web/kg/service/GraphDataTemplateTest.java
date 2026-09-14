package com.iisquare.fs.web.kg.service;

import com.iisquare.fs.web.kg.ontology.OntologyModel;
import com.iisquare.fs.web.kg.util.ExcelUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 导入模板的表头、示例值与CSV拼接
 */
public class GraphDataTemplateTest {

    protected OntologyModel.Field field(String name, String title, String type, boolean required) {
        OntologyModel.Field field = new OntologyModel.Field();
        field.setName(name);
        field.setTitle(title);
        field.setType(type);
        field.setRequired(required);
        return field;
    }

    protected OntologyModel.Entity entity(String name, String label, String primaryField, OntologyModel.Field... fields) {
        OntologyModel.Entity entity = new OntologyModel.Entity();
        entity.setName(name);
        entity.setLabel(label);
        entity.setPrimaryField(primaryField);
        entity.setFields(new ArrayList<>(Arrays.asList(fields)));
        return entity;
    }

    @Test
    public void entityTemplateUsesTitlesAndSampleValues() {
        OntologyModel.Entity company = entity("企业", "Company", "code",
                field("code", "编码", "String", true),
                field("name", "名称", "String", true),
                field("employees", "员工数", "Integer", false));
        GraphDataService service = new GraphDataService();
        ExcelUtil.SheetData sheet = service.entitySheet(company);
        Assert.assertEquals("企业(Company)", sheet.name);
        Assert.assertEquals(Arrays.asList("编码", "名称", "员工数"), sheet.headers);
        Assert.assertEquals(Arrays.asList("ID001", "示例名称", 1), sheet.rows.get(0));
    }

    @Test
    public void relationshipTemplateContainsEndpoints() {
        OntologyModel.Entity company = entity("企业", "Company", "code", field("code", "编码", "String", true));
        OntologyModel.Entity person = entity("人员", "Person", "id", field("id", "标识", "String", true));
        OntologyModel.Relationship relationship = new OntologyModel.Relationship();
        relationship.setName("任职");
        relationship.setLabel("WORKS_FOR");
        relationship.setSourceEntity(company);
        relationship.setTargetEntity(person);
        relationship.setFields(new ArrayList<>(Arrays.asList(field("startYear", "入职年份", "Integer", false))));
        ExcelUtil.SheetData sheet = new GraphDataService().relationshipSheet(relationship);
        Assert.assertEquals(Arrays.asList("source", "target", "入职年份"), sheet.headers);
        Assert.assertEquals("ID001", sheet.rows.get(0).get(0));
        Assert.assertEquals("ID002", sheet.rows.get(0).get(1));
    }

    @Test
    public void headerFallsBackToFieldNameWhenTitleConflicts() {
        OntologyModel.Entity company = entity("企业", "Company", "code",
                field("code", "编码", "String", true),
                field("name", "编码", "String", false));
        List<String> headers = new GraphDataService().fieldHeaders(company.getFields());
        Assert.assertEquals(Arrays.asList("code", "name"), headers);
    }

    @Test
    public void csvCellEscapesCommaAndQuote() {
        GraphDataService service = new GraphDataService();
        Assert.assertEquals("\"值1,值2\"", service.csvCell("值1,值2"));
        Assert.assertEquals("\"say \"\"hi\"\"\"", service.csvCell("say \"hi\""));
        Assert.assertEquals("普通文本", service.csvCell("普通文本"));
    }

}
