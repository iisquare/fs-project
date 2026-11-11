package com.iisquare.fs.app.spark.node;

import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 字段条件过滤节点，同时适用于 Spark 批处理和 Structured Streaming。
 */
public class FilterTransformNode extends AbstractConvertTransform<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() {
        Dataset<Row> dataset = null;
        List<Dataset<Row>> inputs = inputs(SparkTypes.DATASET);
        for (Dataset<Row> input : inputs) {
            dataset = null == dataset ? input : dataset.union(input);
        }
        if (null == dataset) throw new IllegalArgumentException("条件过滤节点缺少上游数据");
        String field = options.at("/field").asText("").trim();
        if (DPUtil.empty(field)) throw new IllegalArgumentException("条件过滤节点未配置过滤字段");
        String operator = options.at("/operator").asText("EQ").trim().toUpperCase(Locale.ROOT);
        String expected = options.at("/value").asText("");
        String valueType = options.at("/valueType").asText("string").toLowerCase(Locale.ROOT);
        boolean caseSensitive = options.at("/caseSensitive").asBoolean(true);
        return dataset.filter(condition(functions.col(field), operator, expected, valueType, caseSensitive));
    }

    static Column condition(Column column, String operator, String expected,
                            String valueType, boolean caseSensitive) {
        switch (operator) {
            case "IS_NULL":
                return column.isNull();
            case "IS_NOT_NULL":
                return column.isNotNull();
            case "IN": {
                List<Object> values = new ArrayList<>();
                for (String item : expected.split("\\s*,\\s*")) {
                    values.add(literal(item, valueType, caseSensitive));
                }
                return normalized(column, valueType, caseSensitive)
                        .isin(values.toArray(new Object[0]));
            }
            case "REGEX":
                return column.rlike(caseSensitive || !"string".equals(valueType)
                        ? expected : "(?i)" + expected);
            default:
                break;
        }
        Column target = normalized(column, valueType, caseSensitive);
        if ("CONTAINS".equals(operator)
                || "STARTS_WITH".equals(operator)
                || "ENDS_WITH".equals(operator)) {
            if (!"string".equals(valueType)) {
                throw new IllegalArgumentException("包含类条件仅支持字符串值");
            }
            Column literal = normalized(functions.lit(expected), valueType, caseSensitive);
            switch (operator) {
                case "CONTAINS":
                    return target.contains(literal);
                case "STARTS_WITH":
                    return target.startsWith(literal);
                default:
                    return target.endsWith(literal);
            }
        }
        Column value = functions.lit(literal(expected, valueType, caseSensitive));
        switch (operator) {
            case "EQ":
                return target.equalTo(value);
            case "NE":
                return target.notEqual(value);
            case "GT":
                return target.gt(value);
            case "GE":
                return target.geq(value);
            case "LT":
                return target.lt(value);
            case "LE":
                return target.leq(value);
            default:
                throw new IllegalArgumentException("不支持的过滤条件：" + operator);
        }
    }

    private static Column normalized(Column column, String valueType, boolean caseSensitive) {
        return "string".equals(valueType) && !caseSensitive ? functions.lower(column) : column;
    }

    private static Object literal(String value, String valueType, boolean caseSensitive) {
        if ("number".equals(valueType)) return DPUtil.parseDouble(value);
        if ("boolean".equals(valueType)) return DPUtil.parseBoolean(value);
        return caseSensitive ? value : value.toLowerCase(Locale.ROOT);
    }
}
