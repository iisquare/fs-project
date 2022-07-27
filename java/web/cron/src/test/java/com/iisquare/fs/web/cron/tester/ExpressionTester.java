package com.iisquare.fs.web.cron.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.util.DAGUtil;
import org.junit.Test;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExpressionTester {

    @Test
    public void spELTest() {
        ExpressionParser parser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext();
        context.setVariable("name", "test");
        context.setVariable("age", 2);
        context.setVariable("map", new LinkedHashMap<String, Object>(){{
            put("a", 1);
            put("b", 3);
            put("c", "test");
        }});

        Expression expression = parser.parseExpression("#age > 3 || #name == #map['c']");
        System.out.println(expression.getValue(context, Boolean.class));
    }

    @Test
    public void jsonTest() {
        ObjectNode config = DPUtil.objectNode();
        config.put("a", 1);
        config.put("b", 2);
        config.put("c", "hello");
        String condition = "1 + a == c";

        Map data = DPUtil.toJSON(config, Map.class);
        StandardEvaluationContext context = new StandardEvaluationContext(data);
        context.addPropertyAccessor(new MapAccessor());

        SpelExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(condition);
        Object result = exp.getValue(context);
        System.out.println("result:" + result);
    }

    @Test
    public void formatTest() {
        ObjectNode options = DPUtil.objectNode();
        options.put("command", "tasklist -fi");
        ObjectNode config = DPUtil.objectNode();
        System.out.println("options -> " + options.toPrettyString());
        System.out.println("config -> " + config.toPrettyString());
        JsonNode formatted = DAGUtil.formatOptions(options, config);
        System.out.println("formatted -> " + formatted.toPrettyString());
    }

}
