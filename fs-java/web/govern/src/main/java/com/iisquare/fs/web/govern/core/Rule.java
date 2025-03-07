package com.iisquare.fs.web.govern.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.govern.entity.QualityRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public abstract class Rule {

    protected QualityRule entity;
    protected JsonNode options;
    protected Connection connection;
    public int checkCount = 0;
    public int hitCount = 0;
    public String expression = "";

    public static Map<String, Object> entity(Connection connection, QualityRule entity) {
        String name = Rule.class.getPackage().getName();
        name = name.replaceFirst("\\.core", ".rule.") + entity.getType();
        Rule rule;
        try {
            Class<?> cls = Class.forName(name);
            rule = (Rule) cls.newInstance();
        } catch (Exception e) {
            return ApiUtil.result(5001, "获取规则执行器异常", e);
        }
        rule.entity = entity;
        rule.options = DPUtil.parseJSON(entity.getContent());
        rule.connection = connection;
        try {
            return rule.execute();
        } catch (Exception e) {
            return ApiUtil.result(5002, "执行规则校验异常", e);
        }
    }

    public abstract Map<String, Object> execute() throws Exception;

    public String checkWhere() {
        String where = entity.getCheckWhere();
        if (DPUtil.empty(where)) return "1 = 1";
        return where;
    }

    public String referWhere() {
        String where = entity.getReferWhere();
        if (DPUtil.empty(where)) return "1 = 1";
        return where;
    }

    public int resultInteger(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql + " limit 1");
        int result = 0;
        while (rs.next()) {
            result = rs.getInt(1);
            break;
        }
        FileUtil.close(rs, statement);
        return result;
    }

    public Map<String, Object> result() {
        String suggest = entity.getSuggest();
        suggest.replaceAll("\\$\\{checkTable\\}", entity.getCheckTable());
        suggest.replaceAll("\\$\\{checkColumn\\}", entity.getCheckColumn());
        suggest.replaceAll("\\$\\{referTable\\}", entity.getReferTable());
        suggest.replaceAll("\\$\\{referColumn\\}", entity.getReferColumn());
        ObjectNode result = DPUtil.objectNode();
        result.put("expression", expression);
        result.put("checkCount", checkCount);
        result.put("hitCount", hitCount);
        return ApiUtil.result(0, suggest, result);
    }

    public String checkMetric() {
        return metric(entity.getCheckMetric(), entity.getCheckColumn());
    }

    public String referMetric() {
        return metric(entity.getReferMetric(), entity.getReferColumn());
    }

    public String metric(String metric, String column) {
        if (DPUtil.empty(metric)) return column;
        switch (metric) {
            case "SUM":
            case "COUNT":
            case "MAX":
            case "MIN":
            case "AVG":
                return String.format("%s(%s)", metric, column);
            case "COUNT_DISTINCT":
                return String.format("COUNT(DISTINCT %s)", column);
            default: return column;
        }
    }

    public String operator(String operator) {
        if (DPUtil.empty(operator)) return "=";
        switch (operator) {
            case "NOT_EQUAL": return "!=";
            case "LESS_THAN": return "<";
            case "LESS_THAN_OR_EQUAL": return "<=";
            case "GREATER_THAN": return ">";
            case "GREATER_THAN_OR_EQUAL": return ">=";
            default: return "=";
        }
    }

}
