package com.iisquare.fs.web.kg.schema;

import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构定义解析、语句生成与签名对账
 */
public class SchemaDefinitionTest {

    @Test
    public void createUniqueConstraintOnLabelCombination() {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("kind", "CONSTRAINT");
        param.put("name", "constraint_node_person_employee_id");
        param.put("constraintType", "UNIQUE");
        param.put("ontologyType", "NODE");
        param.put("label", "Person:Employee");
        param.put("fields", "id");
        SchemaDefinition definition = SchemaDefinition.parse(param);
        Assert.assertNull(definition.validate());
        Assert.assertEquals(2, definition.getLabels().size());
        Assert.assertEquals("CREATE CONSTRAINT `constraint_node_person_employee_id` IF NOT EXISTS"
                + " FOR (nor:`Person`:`Employee`) REQUIRE nor.`id` IS UNIQUE", definition.statement());
    }

    @Test
    public void signatureIgnoresLabelOrderAndName() {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("kind", "CONSTRAINT");
        param.put("name", "plan_name");
        param.put("constraintType", "UNIQUE");
        param.put("label", "Person:Employee");
        param.put("fields", "id");
        SchemaDefinition plan = SchemaDefinition.parse(param);
        SchemaDefinition actual = SchemaDefinition.fromConstraintRow(DPUtil.parseJSON(
                "{\"name\":\"other_name\",\"type\":\"UNIQUENESS\",\"entityType\":\"NODE\","
                        + "\"labelsOrTypes\":[\"Employee\",\"Person\"],\"properties\":[\"id\"]}"));
        Assert.assertEquals(plan.signature(), actual.signature());
    }

    @Test
    public void relationshipIndexStatement() {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("name", "index_rel_works_for_start_year");
        param.put("indexType", "RANGE");
        param.put("ontologyType", "REL");
        param.put("label", "WORKS_FOR");
        param.put("fields", "startYear");
        SchemaDefinition definition = SchemaDefinition.parse(param);
        Assert.assertNull(definition.validate());
        Assert.assertEquals("CREATE INDEX `index_rel_works_for_start_year` IF NOT EXISTS"
                + " FOR ()-[nor:`WORKS_FOR`]-() ON (nor.`startYear`)", definition.statement());
    }

    @Test
    public void validateEnterpriseConstraint() {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("kind", "CONSTRAINT");
        param.put("name", "constraint_node_person_name");
        param.put("subType", "NOT_NULL");
        param.put("label", "Person");
        param.put("fields", "name,code");
        SchemaDefinition definition = SchemaDefinition.parse(param);
        Assert.assertEquals("属性存在性约束仅支持单个字段", definition.validate());
        param.put("fields", "name");
        definition = SchemaDefinition.parse(param);
        Assert.assertNull(definition.validate());
        Assert.assertTrue(definition.enterpriseOnly());
    }

    @Test
    public void unmanagedNameAllowsNonAsciiCharacters() {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("kind", "CONSTRAINT");
        param.put("name", "公司_唯一约束");
        SchemaDefinition definition = SchemaDefinition.parse(param);
        Assert.assertNull(definition.validateName());
        Assert.assertEquals("DROP CONSTRAINT `公司_唯一约束` IF EXISTS", definition.dropStatement());
    }

    @Test
    public void nameRejectsBacktickAndOverLength() {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("kind", "INDEX");
        param.put("name", "index_`x`");
        Assert.assertEquals("名称不能包含反引号", SchemaDefinition.parse(param).validateName());
        param.put("name", "index");
        Assert.assertEquals("名称不能为空", SchemaDefinition.parse(new LinkedHashMap<>()).validateName());
        Assert.assertEquals("DROP INDEX `index` IF EXISTS", SchemaDefinition.parse(param).dropStatement());
    }

}
