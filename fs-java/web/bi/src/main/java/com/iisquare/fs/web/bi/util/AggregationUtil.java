package com.iisquare.fs.web.bi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.SQLUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 矩阵、报表计算辅助工具。
 * 将设计器配置转换为 Trino SQL，替换原有基于 Spark 的运算表达式。
 */
public class AggregationUtil {

    private AggregationUtil() {}

    /**
     * 获取启用状态的配置项
     */
    public static ArrayNode enabled(JsonNode array) {
        ArrayNode result = DPUtil.arrayNode();
        if (null == array || !array.isArray()) return result;
        for (JsonNode item : array) {
            if (item.at("/enabled").asBoolean(false)) result.add(item);
        }
        return result;
    }

    /**
     * 反引号标识符转换为 Trino 双引号标识符，仅转换成对反引号包裹的标识符，
     * 避免字符串常量中的孤立反引号被误转换
     */
    public static String quote(String sql) {
        if (null == sql) return null;
        return sql.replaceAll("`([^`]+)`", "\"$1\"");
    }

    /**
     * 字段名称转换为 Trino 标识符，非标识符表达式原样返回
     */
    public static String identifier(String name) {
        if (null == name) return null;
        String value = DPUtil.trim(name);
        if (DPUtil.empty(value)) return value;
        if (value.matches("(`[^`]+`\\.)?`[^`]+`")) return quote(value);
        if (value.matches("(\"[^\"]+\"\\.)?\"[^\"]+\"")) return value;
        if (value.matches("[\\p{L}\\p{N}_\\-]+")) return "\"" + value + "\"";
        return value;
    }

    /**
     * 条件值转换为 Trino 表达式：标识符原样返回，其余作为字符常量
     */
    public static String expression(String expression) {
        if (null == expression) return null;
        String value = DPUtil.trim(expression);
        if (DPUtil.empty(value)) return value;
        if (value.matches("(`[^`]+`\\.)?`[^`]+`")) return quote(value);
        if (value.matches("(\"[^\"]+\"\\.)?\"[^\"]+\"")) return value;
        return literal(expression);
    }

    /**
     * 字符常量转义
     */
    public static String literal(String value) {
        return "'" + escape(value) + "'";
    }

    public static String escape(String value) {
        return null == value ? null : value.replace("'", "''");
    }

    public static String in(String expression) {
        List<String> result = new ArrayList<>();
        for (String exp : DPUtil.explode(",", expression)) {
            result.add(expression(exp));
        }
        return DPUtil.implode(", ", result.toArray(new String[0]));
    }

    /**
     * 过滤条件树转换为 WHERE 表达式，左侧按字段处理、右侧按值或字段引用处理
     */
    public static String filter(JsonNode filter, String glue) {
        if (null == filter || !filter.isArray()) return null;
        List<String> result = new ArrayList<>();
        Iterator<JsonNode> iterator = filter.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (!item.at("/enabled").asBoolean(false)) continue;
            if ("RELATION".equals(item.at("/type").asText())) {
                String children = filter(item.at("/children"), item.at("/value").asText());
                if (!DPUtil.empty(children)) result.add(String.format("(%s)", children));
                continue;
            }
            String left = identifier(item.at("/left").asText()); // 左侧按字段处理，支持直接填写字段名
            String right = expression(item.at("/right").asText());
            String operation = item.at("/value").asText("");
            switch (operation) {
                case "EQUAL":
                    result.add(String.format("%s=%s", left, right));
                    break;
                case "NOT_EQUAL":
                    result.add(String.format("%s<>%s", left, right));
                    break;
                case "LESS_THAN":
                    result.add(String.format("%s<%s", left, right));
                    break;
                case "LESS_THAN_OR_EQUAL":
                    result.add(String.format("%s<=%s", left, right));
                    break;
                case "GREATER_THAN":
                    result.add(String.format("%s>%s", left, right));
                    break;
                case "GREATER_THAN_OR_EQUAL":
                    result.add(String.format("%s>=%s", left, right));
                    break;
                case "IS_NULL":
                    result.add(String.format("%s IS NULL", left));
                    break;
                case "IS_NOT_NULL":
                    result.add(String.format("%s IS NOT NULL", left));
                    break;
                case "LIKE":
                    result.add(String.format("%s LIKE %s", left, right));
                    break;
                case "NOT_LIKE":
                    result.add(String.format("%s NOT LIKE %s", left, right));
                    break;
                case "IN":
                    result.add(String.format("%s IN (%s)", left, in(item.at("/right").asText())));
                    break;
                case "NOT_IN":
                    result.add(String.format("%s NOT IN (%s)", left, in(item.at("/right").asText())));
                    break;
                default:
                    throw new RuntimeException("Filter Expression Operation [" + operation + "] is not supported");
            }
        }
        if (result.isEmpty()) return null;
        glue = DPUtil.empty(glue) ? "AND" : SQLUtil.escape(glue);
        return DPUtil.implode(String.format(" %s ", glue), result.toArray(new String[0]));
    }

    /**
     * 矩阵度量聚合表达式，计数仅统计非空字段
     */
    public static String aggregation(String aggregation, String field) {
        String name = identifier(field);
        switch (aggregation) {
            case "COUNT":
                return String.format("count(%s)", name);
            case "SUM":
                return String.format("sum(%s)", name);
            case "MAX":
                return String.format("max(%s)", name);
            case "MIN":
                return String.format("min(%s)", name);
            case "AVG":
                return String.format("avg(%s)", name);
            case "COUNT_DISTINCT":
                return String.format("count(distinct %s)", name);
            default:
                throw new RuntimeException("Aggregation Operation [" + aggregation + "] is not supported");
        }
    }

    /**
     * 报表度量聚合表达式，计数统计全部记录；过滤条件非空时转为条件聚合，不满足条件的记录不计入结果
     */
    public static String metric(String aggregation, String field, String filter) {
        boolean filtered = !DPUtil.empty(filter);
        switch (aggregation) {
            case "COUNT":
                if (!filtered) return "count(*)";
                return String.format("count_if(%s)", filter);
            case "COUNT_DISTINCT":
                return String.format("count(distinct %s)", condition(field, filtered, filter));
            case "SUM":
            case "MAX":
            case "MIN":
            case "AVG":
                return String.format("%s(%s)", aggregation.toLowerCase(), condition(field, filtered, filter));
            default:
                throw new RuntimeException("Aggregation Operation [" + aggregation + "] is not supported");
        }
    }

    /**
     * 无过滤条件时返回字段标识符，有过滤条件时包裹为 case when 表达式，聚合函数对空值不做统计
     */
    private static String condition(String field, boolean filtered, String filter) {
        String name = identifier(field);
        if (!filtered) return name;
        return String.format("case when (%s) then %s end", filter, name);
    }

    /**
     * 毫秒时间戳日期分段表达式
     */
    public static String date(String field, String interval) {
        switch (interval) {
            case "SECOND":
                return String.format("format_datetime(from_unixtime(%s / 1000), 'yyyy-MM-dd HH:mm:ss')", field);
            case "MINUTE":
                return String.format("format_datetime(from_unixtime(%s / 1000), 'yyyy-MM-dd HH:mm')", field);
            case "HOUR":
                return String.format("format_datetime(from_unixtime(%s / 1000), 'yyyy-MM-dd HH')", field);
            case "DAY":
                return String.format("format_datetime(from_unixtime(%s / 1000), 'yyyy-MM-dd')", field);
            case "MONTH":
                return String.format("format_datetime(from_unixtime(%s / 1000), 'yyyy-MM')", field);
            case "QUARTER":
                return String.format("concat(format_datetime(from_unixtime(%s / 1000), 'yyyy'), '-Q', cast(quarter(from_unixtime(%s / 1000)) as varchar))", field, field);
            case "YEAR":
                return String.format("format_datetime(from_unixtime(%s / 1000), 'yyyy')", field);
            default:
                return field;
        }
    }

    /**
     * 合并 WHERE 条件，返回包含 where 关键字的语句片段
     */
    public static String where(String... parts) {
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (null == part || DPUtil.empty(part)) continue;
            result.add("(" + part + ")");
        }
        if (result.isEmpty()) return "";
        return " where " + DPUtil.implode(" and ", result.toArray(new String[0]));
    }

}
