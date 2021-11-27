package com.iisquare.fs.web.bi.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VisualizeUnit {

    public static Map<String, Object> visualize(SparkSession session, JsonNode dataset, JsonNode options, JsonNode levels) {
        try {
            Map<String, Object> result = SourceUnit.loadSource(session, dataset);
            if (ApiUtil.failed(result)) return result;
            result = DatasetUnit.sql(dataset);
            if (ApiUtil.failed(result)) return result;
            return axis(session.sql(ApiUtil.data(result, String.class)), options, levels);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiUtil.result(30500, e.getMessage(), null);
        }
    }

    public static Map<String, Object> levels(JsonNode options, JsonNode levels) {
        List<String> result = new ArrayList<>();
        JsonNode buckets = options.at("/axis/buckets");
        Iterator<JsonNode> iterator = levels.iterator();
        int levelIndex = 0;
        while (iterator.hasNext()) {
            JsonNode level = iterator.next();
            JsonNode bucket = buckets.get(levelIndex);
            if (null == bucket) return ApiUtil.result(71001, "获取层级维度条件失败", levelIndex);
            String aggregation = bucket.at("/aggregation").asText("");
            switch (aggregation) {
                case "TERM":
                case "HISTOGRAM":
                case "DATE_HISTOGRAM":
                    String field = bucket.at("/field").asText();
                    String interval = bucket.at("/interval").asText("");
                    if (DPUtil.empty(field)) {
                        return ApiUtil.result(71003, "维度字段配置异常", level);
                    }
                    if ("HISTOGRAM".equals(aggregation)) {
                        int divider = DPUtil.parseInt(interval);
                        if (Math.abs(divider) > 1) field = String.format("floor(%s / %d)", field, divider);
                    } else if ("DATE_HISTOGRAM".equals(aggregation)) {
                        field = date(field, interval);
                    }
                    result.add(String.format("((%s)='%s')", field, level.at("/x").asText("")));
                    break;
                case "FILTER":
                    JsonNode ft = bucket.at("/filters").get(level.at("/index").asInt(-1));
                    if (null == ft) return ApiUtil.result(71004, "维度过滤配置异常", level);
                    String filter = DatasetUnit.filter(ft.at("/filter"), null);
                    if (!DPUtil.empty(filter)) result.add(String.format("(%s)", filter));
                    break;
                default:
                    return ApiUtil.result(71002, "维度类型暂不支持", aggregation);
            }
            levelIndex++;
        }
        return ApiUtil.result(0, null, DPUtil.implode(" AND ", result.toArray(new String[0])));
    }

    public static Map<String, Object> bucket(Dataset<Row> dataset, JsonNode bucket, ObjectNode x) {
        if (null == bucket || !bucket.isObject()) {
            return ApiUtil.result(61001, "获取所在层级维度配置异常", null);
        }
        String aggregation = bucket.at("/aggregation").asText("");
        String interval = bucket.at("/interval").asText("");
        x.put("aggregation", aggregation).put("interval", interval);
        x.put("label", bucket.at("/label").asText()); // 层级名称
        ArrayNode data = x.putArray("data");
        List<Dataset<Row>> list = new ArrayList<>();
        Iterator<JsonNode> iterator = null;
        switch (aggregation) {
            case "TERM":
            case "HISTOGRAM":
            case "DATE_HISTOGRAM":
                String field = bucket.at("/field").asText();
                if (DPUtil.empty(field)) {
                    return ApiUtil.result(61003, "维度字段配置异常", null);
                }
                if ("HISTOGRAM".equals(aggregation)) {
                    int divider = DPUtil.parseInt(interval);
                    if (Math.abs(divider) > 1) field = String.format("floor(%s / %d)", field, divider);
                } else if ("DATE_HISTOGRAM".equals(aggregation)) {
                    field = date(field, interval);
                }
                Dataset<Row> distinct = dataset.selectExpr(field).distinct();
                List<Row> values = distinct.sort(distinct.col(distinct.columns()[0]).asc()).collectAsList();
                for (Row row : values) {
                    String label = row.get(0).toString();
                    data.add(label);
                    list.add(dataset.where(String.format("(%s)='%s'", field, label)));
                }
                break;
            case "FILTER":
                iterator = bucket.at("/filters").iterator();
                while (iterator.hasNext()) {
                    JsonNode ft = iterator.next();
                    data.add(ft.at("/label").asText());
                    String filter = DatasetUnit.filter(ft.at("/filter"), null);
                    list.add(DPUtil.empty(filter) ? dataset : dataset.where(filter));
                }
                break;
            default:
                return ApiUtil.result(61002, "维度类型暂不支持", aggregation);
        }
        return ApiUtil.result(0, null, list);
    }

    public static Map<String, Object> axis(Dataset<Row> dataset, JsonNode options, JsonNode levels) {
        if (null == levels || !levels.isArray()) levels = DPUtil.arrayNode();
        String filter = DatasetUnit.filter(options.at("/filter"), null);
        if (!DPUtil.empty(filter)) dataset = dataset.where(filter);
        Map<String, Object> result = levels(options, levels);
        if (ApiUtil.failed(result)) return result;
        filter = ApiUtil.data(result, String.class);
        if (!DPUtil.empty(filter)) dataset = dataset.where(filter);
        JsonNode bucket = options.at("/axis/buckets").get(levels.size());
        ObjectNode axis = DPUtil.objectNode();
        result = bucket(dataset, bucket, axis.putObject("x"));
        if (ApiUtil.failed(result)) return result;
        result = metrics(ApiUtil.data(result, List.class), options.at("/axis/metrics"), axis.putArray("y"));
        if (ApiUtil.failed(result)) return result;
        axis.put("xSize", options.at("/axis/buckets").size());
        axis.put("ySize", options.at("/axis/metrics").size());
        axis.replace("levels", levels); // 请求的钻取历史
        return ApiUtil.result(0, null, axis);
    }

    public static String date(String field, String interval) {
        switch (interval) {
            case "SECOND": return String.format("from_unixtime(%s / 1000, 'yyyy-MM-dd HH:mm:ss')", field);
            case "MINUTE": return String.format("from_unixtime(%s / 1000, 'yyyy-MM-dd HH:mm')", field);
            case "HOUR": return String.format("from_unixtime(%s / 1000, 'yyyy-MM-dd HH')", field);
            case "DAY": return String.format("from_unixtime(%s / 1000, 'yyyy-MM-dd')", field);
            case "MONTH": return String.format("from_unixtime(%s / 1000, 'yyyy-MM')", field);
            case "QUARTER": return String.format("concat(from_unixtime(%s / 1000, 'yyyy'), '-Q', quarter(from_unixtime(%s / 1000, 'yyyy-MM-dd')))", field, field);
            case "YEAR": return String.format("from_unixtime(%s / 1000, 'yyyy')", field);
            default: return field;
        }
    }

    public static Map<String, Object> metrics(List<Dataset<Row>> list, JsonNode metrics, ArrayNode y) {
        ArrayNode result = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = metrics.iterator();
        while (iterator.hasNext()) {
            JsonNode metric = iterator.next();
            ObjectNode item = y.addObject();
            item.put("label", metric.at("/label").asText());
            String field = metric.at("/field").asText("");
            String aggregation = metric.at("/aggregation").asText("");
            switch (aggregation) {
                case "COUNT":
                    aggregation += "(*)";
                    break;
                case "SUM": case "MAX": case "MIN": case "AVG":
                    aggregation = String.format("%s(%s)", aggregation, field);
                    break;
                case "COUNT_DISTINCT":
                    aggregation = String.format("%s(DISTINCT %s)", aggregation, field);
                    break;
            }
            ArrayNode data = item.putArray("data");
            String filter = DatasetUnit.filter(metric.at("/filter"), null);
            for (Dataset<Row> dataset : list) {
                if (!DPUtil.empty(filter)) dataset = dataset.where(filter);
                Row row = dataset.selectExpr(aggregation).collectAsList().get(0);
                data.add(DPUtil.toJSON(row.get(0)));
            }
        }
        return null;
    }

}
