package com.iisquare.fs.app.flink.node;

import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.DAGElementUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 字段条件过滤节点，同时适用于 Flink 流处理和批处理。
 *
 * Flink 上游可能是 Row、JsonNode 或 JSON 字符串，因此按运行时元素类型提取字段。
 * Row 依据上游 RowTypeInfo 的字段名定位，JSON 数据支持点路径（如 after.status）。
 */
public class FilterTransformNode extends AbstractConvertTransform<DataStream<Object>> {

    @Override
    public DAGType<DataStream<Object>> outputType() {
        return FlinkTypes.DATASTREAM_OBJECT;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DataStream<Object> process() {
        DataStream<Object> stream = (DataStream<Object>) (DataStream<?>) input(0, FlinkTypes.DATASTREAM_OBJECT);
        String field = options.at("/field").asText("").trim();
        if (DPUtil.empty(field)) throw new IllegalArgumentException("条件过滤节点未配置过滤字段");
        String operator = normalizedOperator(options.at("/operator").asText("EQ"));
        String expected = options.at("/value").asText("");
        String valueType = options.at("/valueType").asText("string").toLowerCase(Locale.ROOT);
        boolean caseSensitive = options.at("/caseSensitive").asBoolean(true);
        TypeInformation<?> type = ((DataStream<?>) stream).getType();
        RowTypeInfo rowType = type instanceof RowTypeInfo ? (RowTypeInfo) type : null;
        SingleOutputStreamOperator<Object> result = stream.filter((FilterFunction<Object>) element ->
                matches(DAGElementUtil.fieldValue(element, rowType, field),
                        operator, expected, valueType, caseSensitive));
        return result.name(getClass().getSimpleName()).setParallelism(parallelism);
    }

    static boolean matches(Object actual, String operator, String expected,
                           String valueType, boolean caseSensitive) {
        switch (operator) {
            case "IS_NULL":
                return null == actual;
            case "IS_NOT_NULL":
                return null != actual;
            case "IN":
                return Arrays.stream(expected.split("\\s*,\\s*"))
                        .anyMatch(item -> equal(actual, item, valueType, caseSensitive));
            default:
                break;
        }
        if (null == actual) return false;
        switch (operator) {
            case "EQ":
                return equal(actual, expected, valueType, caseSensitive);
            case "NE":
                return !equal(actual, expected, valueType, caseSensitive);
            case "GT":
                return compare(actual, expected, valueType, caseSensitive) > 0;
            case "GE":
                return compare(actual, expected, valueType, caseSensitive) >= 0;
            case "LT":
                return compare(actual, expected, valueType, caseSensitive) < 0;
            case "LE":
                return compare(actual, expected, valueType, caseSensitive) <= 0;
            case "CONTAINS":
                return normalize(actual, caseSensitive).contains(normalize(expected, caseSensitive));
            case "STARTS_WITH":
                return normalize(actual, caseSensitive).startsWith(normalize(expected, caseSensitive));
            case "ENDS_WITH":
                return normalize(actual, caseSensitive).endsWith(normalize(expected, caseSensitive));
            case "REGEX":
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                return Pattern.compile(expected, flags).matcher(String.valueOf(actual)).find();
            default:
                throw new IllegalArgumentException("不支持的过滤条件：" + operator);
        }
    }

    private static boolean equal(Object actual, String expected, String valueType, boolean caseSensitive) {
        if ("number".equals(valueType)) {
            BigDecimal left = decimal(actual);
            BigDecimal right = decimal(expected);
            return null != left && null != right && 0 == left.compareTo(right);
        }
        if ("boolean".equals(valueType)) {
            return DPUtil.parseBoolean(actual) == DPUtil.parseBoolean(expected);
        }
        return normalize(actual, caseSensitive).equals(normalize(expected, caseSensitive));
    }

    private static int compare(Object actual, String expected, String valueType, boolean caseSensitive) {
        if ("number".equals(valueType)) {
            BigDecimal left = decimal(actual);
            BigDecimal right = decimal(expected);
            if (null == left || null == right) throw new IllegalArgumentException("过滤值不是有效数字：" + expected);
            return left.compareTo(right);
        }
        return normalize(actual, caseSensitive).compareTo(normalize(expected, caseSensitive));
    }

    private static BigDecimal decimal(Object value) {
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalize(Object value, boolean caseSensitive) {
        String text = String.valueOf(value);
        return caseSensitive ? text : text.toLowerCase(Locale.ROOT);
    }

    private static String normalizedOperator(String operator) {
        return DPUtil.empty(operator) ? "EQ" : operator.trim().toUpperCase(Locale.ROOT);
    }
}
