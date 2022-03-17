package com.iisquare.fs.web.cron.tester;

import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.LinkedHashMap;

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

}
