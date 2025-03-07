package com.iisquare.fs.web.govern.rule;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.govern.core.Rule;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * 正则校验
 * 检查字段值是否匹配正则表达式
 */
public class RegularRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("select ").append(entity.getCheckColumn()).append(" from ").append(entity.getCheckTable());
        sql.append(" where ").append(checkWhere());
        this.expression = sql.toString();
        String regex = options.at("/expression").asText();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(this.expression)) {
            while (rs.next()) {
                this.checkCount++;
                String str = DPUtil.parseString(rs.getObject(1));
                try {
                    if (DPUtil.isMatcher(regex, str)) this.hitCount++;
                } catch (Exception e) {}
            }
        }
        return result();
    }
}
