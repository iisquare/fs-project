package com.iisquare.fs.web.govern.rule;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.govern.core.Rule;

import java.util.Map;

/**
 * 常量比较
 * 按分组字段对比计算值和常量
 */
public class ConstCompareRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        String checkGroup = entity.getCheckGroup();
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ").append(entity.getCheckTable());
        sql.append(" where ").append(checkWhere());
        this.checkCount = resultInteger(sql.toString());
        sql = new StringBuilder();
        if (DPUtil.empty(checkGroup)) { // 不分组
            sql.append("select count(*) from (");
            sql.append("  select ").append(checkMetric()).append(" as mc ");
            sql.append("  from ").append(entity.getCheckTable()).append(" where ").append(checkWhere());
            sql.append(") as t where t.mc ").append(operator(options.at("/operator").asText()));
            sql.append(" ").append(options.at("/value").asText());
        } else {
            sql.append("select count(*) from (");
            sql.append("  select ").append(checkGroup).append(", ").append(checkMetric()).append(" as mc ");
            sql.append("  from ").append(entity.getCheckTable()).append(" where ").append(checkWhere());
            sql.append("  group by ").append(checkGroup);
            sql.append("  having mc ").append(operator(options.at("/operator").asText()))
                    .append(" ").append(options.at("/value").asText());
            sql.append(") as t");
        }
        this.expression = sql.toString();
        this.hitCount = resultInteger(expression);
        return result();
    }
}
