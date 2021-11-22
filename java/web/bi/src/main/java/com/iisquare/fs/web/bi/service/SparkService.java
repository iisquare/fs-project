package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.SQLUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.web.bi.BIApplication;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * 采用临时视图处理SQL查询，用于Session隔离。
 * 每次请求创建新的Session，避免Session之间数据冲突。
 */
@Service
public class SparkService implements InitializingBean, Closeable {

    private SparkSession spark = null;
    @Value("${spark.master:local}")
    private String sparkMaster;

    @Override
    public void afterPropertiesSet() throws Exception {
        SparkConf config = new SparkConf().setAppName(BIApplication.class.getSimpleName());
        config.setMaster(sparkMaster);
        spark = SparkSession.builder().config(config).getOrCreate();
    }

    public List<Double> random() {
        SparkSession session = spark.newSession();
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        JavaSparkContext context = new JavaSparkContext(session.sparkContext());
        JavaRDD<Integer> rdd = context.parallelize(data);
        JavaRDD<Double> result = rdd.map((Function<Integer, Double>) value -> value / Math.random());
        return result.collect();
    }

    public List<Map<String, String>> parseExpressionColumn(String expression) {
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

    public List<Map<String, String>> parseFilterColumn(JsonNode filter) {
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

    public List<Map<String, String>> parseRelationColumn(JsonNode relation) {
        List<Map<String, String>> result = new ArrayList<>();
        Iterator<JsonNode> iterator = relation.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            result.addAll(parseFilterColumn(item.at("/filter")));
        }
        return result;
    }

    public List<Map<String, String>> parseTableColumn(JsonNode table) {
        List<Map<String, String>> result = new ArrayList<>();
        Iterator<JsonNode> iterator = table.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if ("calculate".equals(item.at("/type").asText())) {
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

    public Map<String, Set<String>> tableColumn(List<Map<String, String>> list) {
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

    public Map<String, Object> loadSource(SparkSession session, JsonNode source, JsonNode item, Set<String> columns) {
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

    public Map<String, Object> loadJDBCSource(SparkSession session, JsonNode options, JsonNode item, Set<String> columns) {
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

    public Map<String, Object> loadSource(SparkSession session, JsonNode options) {
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

    public String expression(String expression) {
        if (null == expression) return null;
        if (expression.matches("(`[\\w\\d_\\-]+`\\.)?`[\\w\\d_\\-]+`")) return expression;
        return String.format("'%s'", SQLUtil.escape(expression));
    }

    public String in(String expression) {
        List<String> result = new ArrayList<>();
        for (String exp : DPUtil.explode(expression)) {
            result.add(expression(exp));
        }
        return DPUtil.implode(", ", result.toArray(new String[0]));
    }

    public String filter(JsonNode filter, String glue) {
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

    public Map<String, Object> sql(JsonNode options) {
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

    public String columns(JsonNode table) {
        List<String> result = new ArrayList<>();
        Iterator<JsonNode> iterator = table.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (!item.at("/enabled").asBoolean(false)) continue;
            String name = item.at("/name").asText();
            String expression = String.format("`%s`.`%s`", item.at("/table").asText(), item.at("/column").asText());
            String type = item.at("/type").asText("");
            switch (type) {
                case "":
                    result.add(String.format("%s as %s", expression, name));
                    break;
                case "calculate":
                    String calculate = item.at("/options/expression").asText();
                    if (DPUtil.empty(calculate)) calculate = expression;
                    result.add(String.format("(%s) as %s", calculate, name));
                    break;
                default:
                    throw new RuntimeException("Column [" + type + "] for [" + name + "] not supported");
            }
        }
        return DPUtil.implode(", ", result.toArray(new String[0]));
    }

    public Map<String, Object> dataset(JsonNode options) {
        SparkSession session = spark.newSession();
        try {
            Map<String, Object> result = loadSource(session, options);
            if (ApiUtil.failed(result)) return result;
            result = sql(options);
            if (ApiUtil.failed(result)) return result;
            return query(options, session.sql(ApiUtil.data(result, String.class)));
        } catch (Exception e) {
            return ApiUtil.result(30500, e.getMessage(), null);
        }
    }

    public List<Column> sorter(JsonNode sorter) {
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

    public Map<String, Object> query(JsonNode options, Dataset<Row> dataset) {
        ObjectNode result = DPUtil.objectNode();
        ArrayNode columns = result.putArray("columns");
        Iterator<JsonNode> iterator = options.at("/table").iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (!item.at("/enabled").asBoolean(false)) continue;
            if (!item.at("/viewable").asBoolean(false)) continue;
            ObjectNode column = columns.addObject();
            column.put("name", item.at("/name").asText());
            column.put("title", item.at("/title").asText());
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

    @Override
    @PreDestroy
    public void close() throws IOException {
        if (null != spark) spark.close();
    }
}
