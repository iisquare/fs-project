package com.iisquare.fs.web.govern.rule;

import com.iisquare.fs.web.govern.core.Rule;

import java.util.Map;

/**
 * 范围检查
 * 检查字段值是否在给定的范围中
 */
public class RangeRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ").append(entity.getCheckTable());
        sql.append(" where ").append(checkWhere());
        this.checkCount = resultInteger(sql.toString());
        if (options.at("/minEnable").asBoolean(false)) {
            sql.append(" and ").append(entity.getCheckColumn()).append(" ");
            sql.append(options.at("/minInclude").asBoolean(false) ? ">=" : ">");
            sql.append(" '").append(options.at("/min").asText("")).append("'");
        }
        if (options.at("/maxEnable").asBoolean(false)) {
            sql.append(" and ").append(entity.getCheckColumn()).append(" ");
            sql.append(options.at("/maxInclude").asBoolean(false) ? "<=" : "<");
            sql.append(" '").append(options.at("/max").asText("")).append("'");
        }
        this.expression = sql.toString();
        this.hitCount = resultInteger(expression);
        return result();
    }
}
