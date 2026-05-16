package com.iisquare.fs.web.govern.rule;

import com.iisquare.fs.web.govern.core.Rule;

import java.util.Map;

/**
 * 引用检查
 * 检查字段值是否在给定的关联表中
 */
public class ReferRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from (");
        sql.append("  select ").append(entity.getCheckColumn()).append(" from ").append(entity.getCheckTable());
        sql.append("  where ").append(checkWhere());
        sql.append(") as a ");
        this.checkCount = resultInteger(sql.toString());
        sql.append("inner join (");
        sql.append("  select ").append(entity.getReferColumn()).append(" from ").append(entity.getReferTable());
        sql.append("  where ").append(referWhere());
        sql.append(") as b on a.").append(entity.getCheckColumn()).append(" = b.").append(entity.getReferColumn());
        this.expression = sql.toString();
        this.hitCount = resultInteger(expression);
        return result();
    }
}
