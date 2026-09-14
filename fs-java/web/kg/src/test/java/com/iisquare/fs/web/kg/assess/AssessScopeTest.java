package com.iisquare.fs.web.kg.assess;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * 评估范围得分计算
 */
public class AssessScopeTest {

    @Test
    public void scoreIsAverageOfCheckRates() {
        AssessScope scope = new AssessScope();
        scope.label = "Person";
        scope.name = "人员";
        scope.count = 100;
        scope.add("required", "必填字段[name]缺失", 100, 10, null);
        scope.add("unique", "主键[id]重复", 100, 0, null);
        ObjectNode json = scope.toJson("ENTITY");
        // (90% + 100%) / 2 = 95
        Assert.assertEquals(95D, json.at("/score").asDouble(), 0.001D);
        Assert.assertEquals(10, json.at("/issueCount").asInt());
        Assert.assertEquals(2, json.at("/items").size());
    }

    @Test
    public void sampledCheckIsMarked() {
        AssessScope scope = new AssessScope();
        scope.label = "Person";
        scope.count = 5000;
        scope.add(true, "type", "属性类型与定义不一致（抽样）", 2000, 25, null);
        ObjectNode json = scope.toJson("ENTITY");
        Assert.assertTrue(json.at("/items/0/sampled").asBoolean());
        Assert.assertEquals(98.75D, json.at("/score").asDouble(), 0.001D);
    }

    @Test
    public void emptyScopeUsesFullScore() {
        AssessScope scope = new AssessScope();
        scope.label = "Person";
        ObjectNode json = scope.toJson("ENTITY");
        Assert.assertEquals(100D, json.at("/score").asDouble(), 0.001D);
        Assert.assertEquals(0, json.at("/issueCount").asInt());
    }

}
