package com.iisquare.fs.web.spark.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.sql.*;

import java.util.*;

public class MatrixUnit {

    public static Map<String, Object> matrix(SparkSession session, JsonNode dataset, JsonNode options) {
        try {
            Map<String, Object> result = SourceUnit.loadSource(session, dataset);
            if (ApiUtil.failed(result)) return result;
            result = DatasetUnit.sql(dataset);
            if (ApiUtil.failed(result)) return result;
            return aggregation(session.sql(ApiUtil.data(result, String.class)), options);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiUtil.result(30500, e.getMessage(), null);
        }
    }

    public static Map<String, Object> aggregation (Dataset<Row> dataset, JsonNode options) {
        String filter = DatasetUnit.filter(options.at("/filter"), null);
        if (!DPUtil.empty(filter)) dataset = dataset.where(filter);
        ObjectNode agg = DPUtil.objectNode();
        Map<String, Object> result = buckets(dataset, agg, options.at("/aggregation/buckets"));
        if (ApiUtil.failed(result)) return result;
        List<Column> buckets = ApiUtil.data(result, List.class);
        result = metrics(agg, options.at("/aggregation/metrics"));
        if (ApiUtil.failed(result)) return result;
        List<Column> metrics = ApiUtil.data(result, List.class);
        List<String> wheres = new ArrayList<>(); // 递归的层级列筛选
        List<String> roads = new ArrayList<>(); // 递归的层级路径
        result = levels(dataset, agg, enabled(options.at("/aggregation/levels")), 0, buckets, metrics, wheres, roads);
        if (ApiUtil.failed(result)) return result;
        agg.replace("matrix", ApiUtil.data(result, ArrayNode.class));
        return ApiUtil.result(0, null, agg);
    }

    public static ArrayNode enabled(JsonNode array) {
        ArrayNode result = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (item.at("/enabled").asBoolean(false)) {
                result.add(item);
            }
        }
        return result;
    }

    public static Map<String, Object> levels(Dataset<Row> dataset, ObjectNode agg, JsonNode levels, int levelIndex, List<Column> buckets, List<Column> metrics, List<String> wheres, List<String> roads) {
        ArrayNode matrix = DPUtil.arrayNode(); // 当前层的列数据
        int levelSize = levels.size();
        if (levelIndex >= levelSize) return ApiUtil.result(0, null, matrix); // 递归结束
        JsonNode level = levels.get(levelIndex);
        String expression = level.at("/expression").asText();
        if (DPUtil.empty(expression)) {
            String name = level.at("/name").asText();
            if (!DPUtil.empty(name)) expression = String.format("`%s`", name);
        }
        if (DPUtil.empty(expression)) return ApiUtil.result(71001, "层级字段配置异常", level);
        ArrayNode schema = agg.has("levels") ? (ArrayNode) agg.get("levels") : agg.putArray("levels"); // 每层的标签名称
        if (!schema.has(levelIndex)) {
            schema.addObject().put("label", level.at("/title").asText());
        }
        ArrayNode y = agg.has("y") ? (ArrayNode) agg.get("y") : agg.putArray("y"); // 聚合数据
        Dataset<Row> distinct = dataset.selectExpr(expression).distinct();
        Column column = distinct.col(distinct.columns()[0]);
        Dataset<Row> data = distinct.sort("asc".equals(level.at("/sort").asText("asc")) ? column.asc() : column.desc());
        List<Row> values = data.collectAsList();
        for (Row row : values) {
            String value = row.get(0).toString();
            List<String> road = clone(roads, value);
            List<String> where = clone(wheres, String.format("(%s)='%s'", expression, value));
            Dataset<Row> next = dataset.where(DPUtil.implode(" AND ", where.toArray(new String[0])));
            Map<String, Object> children = levels(next, agg, levels, levelIndex + 1, buckets, metrics, where, road);
            if (ApiUtil.failed(children)) return children;
            matrix.addObject().put("label", value).replace("children", ApiUtil.data(children, ArrayNode.class));
            if (levelIndex < levelSize - 1) continue; // 仅处理最后一层
            // 按原始字段分组 -> 按原始字段聚合 -> 提取别名结果
            Dataset<Row> result = SparkUtil.agg(next.groupBy(buckets.toArray(new Column[0])), metrics);
            ObjectNode collect = y.addObject();
            collect.replace("roads", DPUtil.toJSON(road));
            collect.replace("metrics", SparkUtil.dataset2json(result));
        }
        return ApiUtil.result(0, null, matrix);
    }

    public static List<String> clone(Collection<String> list, String... items) {
        List<String> result = new ArrayList<>();
        result.addAll(list);
        result.addAll(Arrays.asList(items));
        return result;
    }

    public static Map<String, Object> metrics(ObjectNode agg, JsonNode metrics) {
        Iterator<JsonNode> iterator = metrics.iterator();
        int index = 0;
        List<Column> columns = new ArrayList<>(); // function col with alias
        ArrayNode schema = agg.putArray("metrics");
        while (iterator.hasNext()) {
            JsonNode metric = iterator.next();
            if (!metric.at("/enabled").asBoolean(false)) continue;
            String key = "metric_" + index++;
            String name = metric.at("/name").asText();
            if (DPUtil.empty(name)) return ApiUtil.result(72001, "度量字段配置异常", metric);
            String aggregation = metric.at("/aggregation").asText("");
            switch (aggregation) {
                case "COUNT":
                    columns.add(functions.count(name).alias(key));
                    break;
                case "SUM":
                    columns.add(functions.sum(name).alias(key));
                    break;
                case "MAX":
                    columns.add(functions.max(name).alias(key));
                    break;
                case "MIN":
                    columns.add(functions.min(name).alias(key));
                    break;
                case "AVG":
                    columns.add(functions.avg(name).alias(key));
                    break;
                case "COUNT_DISTINCT":
                    columns.add(functions.count_distinct(new Column(name)).alias(key));
                    break;
                default:
                    throw new RuntimeException("Aggregation Operation [" + aggregation + "] is not supported");
            }
            schema.addObject().put("key", key).put("label", metric.at("/title").asText());
        }
        if (columns.size() == 0) return ApiUtil.result(72002, "无有效度量字段", null);
        return ApiUtil.result(0, null, columns);
    }

    public static Map<String, Object> buckets(Dataset<Row> dataset, ObjectNode agg, JsonNode buckets) {
        Iterator<JsonNode> iterator = buckets.iterator();
        int index = 0;
        List<Column> columns = new ArrayList<>(); // name col with alias
        List<Column> sorts = new ArrayList<>();
        ArrayNode schema = agg.putArray("buckets");
        while (iterator.hasNext()) {
            JsonNode bucket = iterator.next();
            if (!bucket.at("/enabled").asBoolean(false)) continue;
            String key = "bucket_" + index++;
            String name = bucket.at("/name").asText();
            if (DPUtil.empty(name)) return ApiUtil.result(71001, "维度字段配置异常", bucket);
            columns.add(new Column(name).alias(key));
            Column column = new Column(key);
            sorts.add("asc".equals(bucket.at("/sort").asText("asc")) ? column.asc() : column.desc());
            schema.addObject().put("key", key).put("label", bucket.at("/title").asText());
        }
        if (columns.size() == 0) return ApiUtil.result(71002, "无有效维度字段", null);
        dataset = dataset.select(columns.toArray(new Column[0])).distinct();
        agg.replace("x", SparkUtil.dataset2json(dataset.sort(sorts.toArray(new Column[0]))));
        return ApiUtil.result(0, null, columns);
    }

}
