package com.iisquare.fs.web.cron.stage;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.cron.core.Stage;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

public class ConditionGatewayStage extends Stage {
    @Override
    public Map<String, Object> call() throws Exception {
        String condition = options.at("/condition").asText();
        if (DPUtil.empty(condition)) {
            return ApiUtil.result(1001, "表达式不能为空", "condition empty!");
        }
        StandardEvaluationContext context = new StandardEvaluationContext(DPUtil.toJSON(config, Map.class));
        context.addPropertyAccessor(new MapAccessor());
        SpelExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(condition);
        Object result = exp.getValue(context);
        int code = DPUtil.empty(result) ? 1 : 0;
        return ApiUtil.result(code, null, DPUtil.stringify(result));
    }
}
