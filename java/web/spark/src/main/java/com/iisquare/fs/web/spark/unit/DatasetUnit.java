package com.iisquare.fs.web.spark.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.SQLUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.*;

public class DatasetUnit {

    public static Map<String, Object> dataset(SparkSession session, JsonNode options) {
        try {
            Map<String, Object> result = SourceUnit.loadSource(session, options);
            if (ApiUtil.failed(result)) return result;
            result = sql(options);
            if (ApiUtil.failed(result)) return result;
            return query(session.sql(ApiUtil.data(result, String.class)), options);
        } catch (Exception e) {
            return ApiUtil.result(30500, e.getMessage(), null);
        }
    }

    public static Map<String, Object> sql(JsonNode options) {
        String columns = columns(options.at("/table"));
        if (DPUtil.empty(columns)) return ApiUtil.result(30601, "无有效查询字段", columns);
        StringBuilder sql = new StringBuilder();
        sql.append("select ").append(columns).append(" from ");
        Set<String> itemIds = new HashSet<>();
        Iterator<JsonNode> iterator = options.at("/relation/items").iterator();
        JsonNode item = iterator.next();
        itemIds.add(item.at("/id").asText());
        sql.append(item.at("/table").asText());
        JsonNode relations = options.at("/relation/relations");
        while (iterator.hasNext()) {
            item = iterator.next();
            sql.append(" JOIN ").append(item.at("/table").asText());
            String id = item.at("/id").asText();
            Iterator<JsonNode> it = relations.iterator();
            while (it.hasNext()) {
                JsonNode relation = it.next();
                String sourceId = relation.at("/sourceId").asText();
                String targetId = relation.at("/targetId").asText();
                if ((itemIds.contains(sourceId) && targetId.equals(id)) || itemIds.contains(targetId) && sourceId.equals(id)) {
                    String filter = filter(relation.at("/filter"), null);
                    if (!DPUtil.empty(filter)) {
                        sql.append(" ON ").append(filter);
                    }
                    break;
                }
            }
        }
        return ApiUtil.result(0, null, sql.toString());
    }

    public static String filter(JsonNode filter, String glue) {
        if (null == filter) return null;
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
            String left = expression(item.at("/left").asText());
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
                case "IN":
                    result.add(String.format("%s IN (%s)", left, in(expression(item.at("/right").asText()))));
                    break;
                case "NOT_IN":
                    result.add(String.format("%s NOT IN (%s)", left, in(expression(item.at("/right").asText()))));
                    break;
                default:
                    throw new RuntimeException("Filter Expression Operation [" + operation + "] is not supported");
            }
        }
        if (result.size() == 0) return null;
        glue = DPUtil.empty(glue) ? "AND" : SQLUtil.escape(glue);
        return DPUtil.implode(String.format(" %s ", glue), result.toArray(new String[0]));
    }

    public static Map<String, Object> query(Dataset<Row> dataset, JsonNode options) {
        ObjectNode result = DPUtil.objectNode();
        ArrayNode columns = result.putArray("columns");
        Iterator<JsonNode> iterator = options.at("/table").iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (!item.at("/enabled").asBoolean(false)) continue;
            ObjectNode column = (ObjectNode) item.deepCopy();
            column.remove("enabled");
            columns.add(column);
        }
        String filter = filter(options.at("/query/filter"), null);
        if (!DPUtil.empty(filter)) dataset = dataset.where(filter);
        long total = dataset.count();
        int page = ValidateUtil.filterInteger(options.at("/query/page").asInt(), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(options.at("/query/pageSize").asInt(), true, 1, 1000, 15);
        result.put("total", total).put("page", page).put("pageSize", pageSize);
        int skip = (page - 1) * pageSize, limit = page * pageSize;
        if (limit > 10000) return ApiUtil.result(30403, "数据长度超过限制", result);
        if (skip > total) {
            result.putArray("rows");
            return ApiUtil.result(0, null, result);
        }
        List<Column> sorter = sorter(options.at("/query/sorter"));
        if (sorter.size() > 0) dataset = dataset.sort(sorter.toArray(new Column[0]));
        dataset = dataset.limit((int) Math.min(limit, total));
        result.replace("rows", DPUtil.subarray(SparkUtil.dataset2json(dataset), skip, pageSize));
        return ApiUtil.result(0, null, result);
    }

    public static List<Column> sorter(JsonNode sorter) {
        List<Column> result = new ArrayList<>();
        Iterator<JsonNode> iterator = sorter.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            Column column = new Column(item.at("/field").asText());
            String direction = item.at("/direction").asText();
            if ("desc".equals(direction)) {
                column = column.desc();
            } else {
                column = column.asc();
            }
            result.add(column);
        }
        return result;
    }

    public static String expression(String expression) {
        if (null == expression) return null;
        if (expression.matches("(`[\\w\\d_\\-]+`\\.)?`[\\w\\d_\\-]+`")) return expression;
        return String.format("'%s'", SQLUtil.escape(expression));
    }

    public static String in(String expression) {
        List<String> result = new ArrayList<>();
        for (String exp : DPUtil.explode(",", expression)) {
            result.add(expression(exp));
        }
        return DPUtil.implode(", ", result.toArray(new String[0]));
    }

    public static String columns(JsonNode table) {
        List<String> result = new ArrayList<>();
        Iterator<JsonNode> iterator = table.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (!item.at("/enabled").asBoolean(false)) continue;
            String name = item.at("/name").asText();
            String expression = String.format("`%s`.`%s`", item.at("/table").asText(), item.at("/column").asText());
            String transform = item.at("/transform").asText("");
            switch (transform) {
                case "":
                case "location":
                    result.add(String.format("%s as %s", expression, name));
                    break;
                case "calculate":
                    String calculate = item.at("/options/expression").asText();
                    if (DPUtil.empty(calculate)) calculate = expression;
                    result.add(String.format("(%s) as %s", calculate, name));
                    break;
                case "cast2string":
                    result.add(String.format("cast(%s as string) as %s", expression, name));
                    break;
                case "cast2integer":
                    result.add(String.format("cast(%s as int) as %s", expression, name));
                    break;
                case "cast2long":
                    result.add(String.format("cast(%s as long) as %s", expression, name));
                    break;
                case "cast2double":
                    result.add(String.format("cast(%s as double) as %s", expression, name));
                    break;
                case "cast2date":
                    String format = item.at("/options/format").asText();
                    if ("millisecond".equals(format)) {
                        result.add(String.format("%s as %s", expression, name));
                    } else if ("second".equals(format)) {
                        result.add(String.format("(%s * 1000) as %s", expression, name));
                    } else {
                        result.add(String.format("(unix_timestamp(%s, '%s') * 1000) as %s", expression, format, name));
                    }
                    break;
                default:
                    throw new RuntimeException("Column [" + transform + "] for [" + name + "] not supported");
            }
        }
        return DPUtil.implode(", ", result.toArray(new String[0]));
    }

}
