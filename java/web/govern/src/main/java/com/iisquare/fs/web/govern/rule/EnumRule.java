package com.iisquare.fs.web.govern.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.SQLUtil;
import com.iisquare.fs.web.govern.core.Rule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 枚举检查
 * 检查字段值是否在给定的枚举值中
 */
public class EnumRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ").append(entity.getCheckTable());
        sql.append(" where ").append(checkWhere());
        this.checkCount = resultInteger(sql.toString());
        JsonNode json = DPUtil.parseJSON(options.at("/json").asText());
        if (null == json || !json.isObject()) {
            return ApiUtil.result(2001, "枚举值字典项配置异常", json);
        }
        List<String> items = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            items.add(String.format("'%s'", SQLUtil.escape(entry.getKey())));
        }
        sql.append(" and ").append(entity.getCheckColumn()).append(" in (");
        sql.append(DPUtil.implode(", ", items)).append(")");
        this.expression = sql.toString();
        this.hitCount = resultInteger(expression);
        return result();
    }
}
