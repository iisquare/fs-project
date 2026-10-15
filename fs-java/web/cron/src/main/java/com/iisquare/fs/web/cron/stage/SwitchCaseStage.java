package com.iisquare.fs.web.cron.stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.cron.core.Stage;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * 条件分支（节点类型：SwitchCase）
 *
 * 分支按先后顺序求值，命中第一个为真的分支，全部不命中时走默认分支；
 * 条件表达式与网关节点同一套 SpEL 口径，可引用流程默认参数、flow、state 等配置项。
 * 输出为命中分支的锚点标识（case-{分支标识} / default），与设计器锚点一致，
 * 调度据此跳过未命中分支上的节点，参见 FlowLogService.branchSkip。
 */
public class SwitchCaseStage extends Stage {

    public static final String DEFAULT_BRANCH = "default";

    public static final String CASE_PREFIX = "case-";

    @Override
    public Map<String, Object> call() throws Exception {
        StandardEvaluationContext context = new StandardEvaluationContext(DPUtil.toJSON(config, Map.class));
        context.addPropertyAccessor(new MapAccessor());
        SpelExpressionParser parser = new SpelExpressionParser();
        for (JsonNode item : options.at("/cases")) {
            String condition = item.at("/condition").asText();
            if (DPUtil.empty(condition)) continue; // 未配置条件的分支不参与命中
            Expression expression = parser.parseExpression(condition);
            if (!DPUtil.parseBoolean(expression.getValue(context))) continue;
            return result(CASE_PREFIX + item.at("/id").asText(""), item.at("/name").asText(""));
        }
        return result(DEFAULT_BRANCH, options.at("/defaultName").asText("否则（默认）"));
    }

    protected Map<String, Object> result(String branch, String name) {
        ObjectNode data = DPUtil.objectNode();
        data.put("branch", branch);
        data.put("branchName", name);
        return ApiUtil.result(0, null, DPUtil.stringify(data));
    }
}
