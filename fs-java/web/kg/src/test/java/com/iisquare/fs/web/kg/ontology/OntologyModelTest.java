package com.iisquare.fs.web.kg.ontology;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * 本体模型解析与校验
 */
public class OntologyModelTest {

    private static final String STRUCTURED = "{\"entities\":["
            + "{\"id\":\"employee\",\"name\":\"员工\",\"label\":\"Employee\",\"labels\":[\"Person\",\"Employee\",\"Staff\"],"
            + "\"primaryField\":\"id\",\"captionField\":\"name\",\"extendable\":true,\"extendableLabels\":true,"
            + "\"fields\":[{\"name\":\"id\",\"title\":\"标识\",\"type\":\"String\",\"required\":true},"
            + "{\"name\":\"name\",\"title\":\"姓名\",\"type\":\"String\"}]}],"
            + "\"relationships\":[{\"id\":\"works\",\"name\":\"任职\",\"label\":\"WORKS_FOR\",\"source\":\"employee\",\"target\":\"employee\","
            + "\"mergeFields\":[\"startYear\"],\"cascadeDelete\":true,"
            + "\"fields\":[{\"name\":\"startYear\",\"title\":\"入职年份\",\"type\":\"Integer\",\"required\":true}]}]}";

    @Test
    public void parseStructuredDefinition() {
        OntologyModel model = OntologyModel.parse(DPUtil.parseJSON(STRUCTURED));
        Assert.assertTrue(model.valid());
        OntologyModel.Entity entity = model.getEntities().get(0);
        Assert.assertEquals("Employee", entity.getLabel());
        Assert.assertEquals(Arrays.asList("Employee", "Person", "Staff"), entity.getLabels());
        Assert.assertTrue(entity.isExtendable());
        Assert.assertTrue(entity.isExtendableLabels());
        Assert.assertTrue(entity.field("id").isRequired());
        OntologyModel.Relationship relationship = model.relationship("WORKS_FOR");
        Assert.assertEquals(Arrays.asList("startYear"), relationship.getMergeFields());
        Assert.assertTrue(relationship.isCascadeDelete());
        Assert.assertEquals("Employee", relationship.getSourceEntity().getLabel());
    }

    @Test
    public void normalizePrimaryLabel() {
        String json = "{\"entities\":[{\"name\":\"客户\",\"labels\":[\"Customer\",\"Person\"],\"primaryField\":\"id\","
                + "\"fields\":[{\"name\":\"id\",\"type\":\"String\"}]}]}";
        OntologyModel.Entity entity = OntologyModel.parse(DPUtil.parseJSON(json)).getEntities().get(0);
        Assert.assertEquals("Customer", entity.getLabel());
        Assert.assertEquals(Arrays.asList("Customer", "Person"), entity.getLabels());
    }

    @Test
    public void detectDuplicateLabelCombination() {
        String json = "{\"entities\":["
                + "{\"label\":\"Person\",\"labels\":[\"Person\",\"Staff\"],\"primaryField\":\"id\",\"fields\":[{\"name\":\"id\",\"type\":\"String\"}]},"
                + "{\"label\":\"Staff\",\"labels\":[\"Staff\",\"Person\"],\"primaryField\":\"id\",\"fields\":[{\"name\":\"id\",\"type\":\"String\"}]}]}";
        OntologyModel model = OntologyModel.parse(DPUtil.parseJSON(json));
        Assert.assertFalse(model.valid());
        boolean matched = false;
        for (String issue : model.getIssues()) {
            if (issue.contains("标签组合[Person:Staff]重复")) matched = true;
        }
        Assert.assertTrue("应检测到标签组合重复", matched);
    }

    @Test
    public void canvasRoundTripKeepsLabels() {
        OntologyModel model = OntologyModel.parse(DPUtil.parseJSON(STRUCTURED));
        JsonNode canvas = model.toCanvasJson();
        OntologyModel round = OntologyModel.parse(canvas);
        OntologyModel.Entity entity = round.getEntities().get(0);
        Assert.assertEquals(Arrays.asList("Employee", "Person", "Staff"), entity.getLabels());
        Assert.assertTrue(entity.isExtendableLabels());
        Assert.assertTrue(entity.field("id").isRequired());
        Assert.assertEquals(Arrays.asList("startYear"), round.relationship("WORKS_FOR").getMergeFields());
        Assert.assertTrue(round.relationship("WORKS_FOR").isCascadeDelete());
    }

}
