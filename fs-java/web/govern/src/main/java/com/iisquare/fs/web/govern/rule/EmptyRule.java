package com.iisquare.fs.web.govern.rule;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.govern.core.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 空值检测
 * 检查字段值是否为NULL或空字符串
 */
public class EmptyRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ").append(entity.getCheckTable());
        sql.append(" where ").append(checkWhere());
        this.checkCount = resultInteger(sql.toString());
        List<String> filter = new ArrayList<>();
        if (options.at("/isNull").asBoolean(false)) {
            filter.add(String.format("%s is null", entity.getCheckColumn()));
        }
        if (options.at("/isBlank").asBoolean(false)) {
            filter.add(String.format("%s = ''", entity.getCheckColumn()));
        }
        sql.append(" and (").append(DPUtil.implode(" or ", filter)).append(")");
        this.expression = sql.toString();
        this.hitCount = resultInteger(expression);
        return result();
    }
}
