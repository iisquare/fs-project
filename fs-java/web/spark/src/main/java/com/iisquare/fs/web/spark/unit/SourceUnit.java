package com.iisquare.fs.web.spark.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.*;

public class SourceUnit {

    public static Map<String, Object> loadSource(SparkSession session, JsonNode source, JsonNode item, Set<String> columns) {
        String type = source.at("/type").asText("");
        ObjectNode options = (ObjectNode) source.at("/options");
        switch (type) {
            case "MySQL":
                options.put("driver", type);
                return loadJDBCSource(session, options, item, columns);
            default:
                return ApiUtil.result(31000, "数据源类型暂不支持", item);
        }
    }

    public static Map<String, Object> loadJDBCSource(SparkSession session, JsonNode options, JsonNode item, Set<String> columns) {
        Map<String, String> cfg = new LinkedHashMap<>();
        String table = item.at("/table").asText();
        if (DPUtil.empty(table)) return ApiUtil.result(31001, "节点表名配置异常", item);
        String column = columns.size() > 0 ? DPUtil.implode(",", columns.toArray(new String[0])) : "*";
        String sql = item.at("/dsl").asText();
        if (DPUtil.empty(sql)) {
            sql = String.format("select %s from %s", column, table);
        } else {
            sql = String.format("select %s from (%s) as %s", column, sql, table);
        }
        cfg.put("url", options.at("/url").asText());
        cfg.put("query", sql);
        cfg.put("driver", DAGCore.jdbcDriver(options.at("/driver").asText()));
        cfg.put("user", options.at("/username").asText());
        cfg.put("password", options.at("/password").asText());
        Dataset<Row> dataset = session.read().format("jdbc").options(cfg).load();
        try {
            dataset.createTempView(table);
        } catch (AnalysisException e) {
            return ApiUtil.result(31500, "创建临时视图失败", item);
        }
        return null;
    }

    public static Map<String, Object> loadSource(SparkSession session, JsonNode options) {
        ObjectNode items = DPUtil.array2object(options.at("/relation/items"), "table");
        JsonNode sources = options.at("/sources");
        List<Map<String, String>> columns = parseRelationColumn(options.at("/relation/relations"));
        columns.addAll(parseTableColumn(options.at("/table")));
        for (Map.Entry<String, Set<String>> entry : tableColumn(columns).entrySet()) {
            JsonNode item = items.at("/" + entry.getKey());
            JsonNode source = sources.at("/" + item.at("/sourceId").asText());
            Map<String, Object> result = loadSource(session, source, item, entry.getValue());
            if (ApiUtil.failed(result)) return result;
        }
        return null;
    }

    public static List<Map<String, String>> parseRelationColumn(JsonNode relation) {
        List<Map<String, String>> result = new ArrayList<>();
        Iterator<JsonNode> iterator = relation.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            result.addAll(parseFilterColumn(item.at("/filter")));
        }
        return result;
    }

    public static List<Map<String, String>> parseTableColumn(JsonNode table) {
        List<Map<String, String>> result = new ArrayList<>();
        Iterator<JsonNode> iterator = table.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if ("calculate".equals(item.at("/transform").asText())) {
                String expression = item.at("/options/expression").asText();
                if (!DPUtil.empty(expression)) {
                    result.addAll(parseExpressionColumn(expression));
                    continue;
                }
            }
            String tableName = item.at("/table").asText();
            String columnName = item.at("/column").asText();
            result.add(DPUtil.buildMap(
                    String.class, String.class,
                    "expression", String.format("`%s`.`%s`", tableName, columnName),
                    "table", tableName,
                    "column", columnName
            ));
        }
        return result;
    }

    public static Map<String, Set<String>> tableColumn(List<Map<String, String>> list) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map> entry : DPUtil.list2map(list, String.class, Map.class, "expression").entrySet()) {
            Map expression = entry.getValue();
            String table = expression.get("table").toString();
            Set<String> columns = result.get(table);
            if (null == columns) {
                columns = new HashSet<>();
                result.put(table, columns);
            }
            columns.add(expression.get("column").toString());
        }
        return result;
    }

    public static List<Map<String, String>> parseExpressionColumn(String expression) {
        List<Map<String, String>> result = new ArrayList<>();
        List<String> matcher = DPUtil.matcher("`([\\w\\d_\\-]+)`\\.`([\\w\\d_\\-]+)`", expression, true);
        int size = matcher.size();
        for (int index = 0; index < size; index += 3) {
            result.add(DPUtil.buildMap(
                    String.class, String.class,
                    "expression", matcher.get(index),
                    "table", matcher.get(index + 1),
                    "column", matcher.get(index + 2)
            ));
        }
        return result;
    }

    public static List<Map<String, String>> parseFilterColumn(JsonNode filter) {
        List<Map<String, String>> result = new ArrayList<>();
        Iterator<JsonNode> iterator = filter.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if ("RELATION".equals(item.at("/type").asText())) {
                result.addAll(parseFilterColumn(item.at("/children")));
            } else {
                result.addAll(parseExpressionColumn(item.at("/left").asText()));
                result.addAll(parseExpressionColumn(item.at("/right").asText()));
            }
        }
        return result;
    }

}
