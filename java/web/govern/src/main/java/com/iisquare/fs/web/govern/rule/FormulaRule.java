package com.iisquare.fs.web.govern.rule;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.govern.core.Rule;

import java.util.Map;

/**
 * 公式校验
 * 检查行记录是否满足自定义表达式
 */
public class FormulaRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ").append(entity.getCheckTable());
        sql.append(" where ").append(checkWhere());
        this.checkCount = resultInteger(sql.toString());
        String formula = options.at("/expression").asText();
        if (DPUtil.empty(formula)) {
            this.expression = sql.toString();
            this.hitCount = this.checkCount;
        } else {
            sql.append(" and ").append(formula);
            this.expression = sql.toString();
            this.hitCount = resultInteger(expression);
        }
        return result();
    }
}
