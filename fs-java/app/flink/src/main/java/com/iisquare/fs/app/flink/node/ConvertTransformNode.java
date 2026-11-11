package com.iisquare.fs.app.flink.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.core.FlinkCore;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.DAGElementUtil;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.types.Row;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Flink 字段映射与类型转换节点，兼容 Row、JsonNode 和 JSON 字符串。
 *
 * 配置与 Spark ConvertTransform 保持一致：
 * items = [{target, source, clsType}]，mode 支持 KEEP_SOURCE、REMOVE_SOURCE、
 * ONLY_TARGET、REMOVE_TARGET。Row 输入输出 Row，JSON 输入统一输出 JsonNode。
 */
public class ConvertTransformNode extends AbstractConvertTransform<DataStream<Object>> {

    @Override
    public DAGType<DataStream<Object>> outputType() {
        return FlinkTypes.DATASTREAM_OBJECT;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DataStream<Object> process() {
        DataStream<Object> stream = FlinkUtil.union(inputs());
        if (null == stream) throw new IllegalArgumentException("字段映射节点缺少上游数据");
        List<Mapping> mappings = mappings(options.at("/items"));
        if (mappings.isEmpty()) return stream;
        String mode = mode(options.at("/mode").asText("KEEP_SOURCE"));
        TypeInformation<?> type = ((DataStream<?>) stream).getType();
        if (type instanceof RowTypeInfo rowType) return convertRow(stream, rowType, mappings, mode);
        return convertJson(stream, mappings, mode);
    }

    private static DataStream<Object> convertRow(DataStream<Object> stream, RowTypeInfo sourceType,
                                                  List<Mapping> mappings, String mode) {
        String[] sourceNames = sourceType.getFieldNames();
        TypeInformation<?>[] sourceTypes = sourceType.getFieldTypes();
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int index = 0; index < sourceNames.length; index++) indexes.put(sourceNames[index], index);
        Set<String> targetNames = new HashSet<>();
        Set<String> sourceNamesUsed = new HashSet<>();
        List<String> targetFields = new ArrayList<>();
        List<Integer> sourceIndexes = new ArrayList<>();
        List<Class<?>> converters = new ArrayList<>();
        List<TypeInformation<?>> targetTypes = new ArrayList<>();
        boolean removeTarget = "REMOVE_TARGET".equals(mode);
        for (Mapping mapping : mappings) {
            targetNames.add(mapping.target);
            sourceNamesUsed.add(mapping.source);
            if (removeTarget) continue;
            Integer index = indexes.get(mapping.source);
            TypeInformation<?> targetType = FlinkCore.type(mapping.clsType);
            if (null == targetType && null != index) targetType = sourceTypes[index];
            if (null == targetType) targetType = Types.GENERIC(Object.class);
            targetFields.add(mapping.target);
            sourceIndexes.add(index);
            converters.add(DAGCore.type(mapping.clsType));
            targetTypes.add(targetType);
        }
        List<String> keptFields = new ArrayList<>();
        List<Integer> keptIndexes = new ArrayList<>();
        for (int index = 0; index < sourceNames.length; index++) {
            String field = sourceNames[index];
            if ("ONLY_TARGET".equals(mode)) break;
            if ("REMOVE_TARGET".equals(mode) || "KEEP_SOURCE".equals(mode)) {
                if (targetNames.contains(field)) continue;
            }
            if ("REMOVE_SOURCE".equals(mode)
                    && (targetNames.contains(field) || sourceNamesUsed.contains(field))) continue;
            keptFields.add(field);
            keptIndexes.add(index);
        }
        List<String> names = new ArrayList<>(targetFields);
        names.addAll(keptFields);
        List<TypeInformation<?>> types = new ArrayList<>(targetTypes);
        for (Integer index : keptIndexes) types.add(sourceTypes[index]);
        RowTypeInfo outputType = new RowTypeInfo(types.toArray(new TypeInformation[0]),
                names.toArray(new String[0]));
        SingleOutputStreamOperator<Row> result = stream.map((MapFunction<Object, Row>) element -> {
            Row source = (Row) element;
            List<Object> values = new ArrayList<>();
            for (int index = 0; index < targetFields.size(); index++) {
                Integer sourceIndex = sourceIndexes.get(index);
                Object value = null == sourceIndex ? null : source.getField(sourceIndex);
                Class<?> converter = converters.get(index);
                values.add(null == converter ? value : DAGCore.convert(converter, value));
            }
            for (Integer index : keptIndexes) values.add(source.getField(index));
            return FlinkUtil.row(values.toArray(new Object[0]));
        }).returns(outputType);
        return (DataStream<Object>) (DataStream<?>) result;
    }

    private static DataStream<Object> convertJson(DataStream<Object> stream,
                                                   List<Mapping> mappings, String mode) {
        Set<String> targetNames = new HashSet<>();
        Set<String> sourceNames = new HashSet<>();
        for (Mapping mapping : mappings) {
            targetNames.add(mapping.target);
            sourceNames.add(mapping.source.split("\\.")[0]);
        }
        SingleOutputStreamOperator<ObjectNode> result = stream.map((MapFunction<Object, ObjectNode>) element -> {
            JsonNode source = DAGElementUtil.toJson(element, null);
            if (!source.isObject()) {
                throw new IllegalArgumentException("字段映射节点的JSON输入必须是对象");
            }
            ObjectNode target = DPUtil.objectNode();
            if ("KEEP_SOURCE".equals(mode) || "REMOVE_TARGET".equals(mode)) {
                source.fields().forEachRemaining(entry -> {
                    if (!targetNames.contains(entry.getKey())) target.set(entry.getKey(), entry.getValue());
                });
            } else if ("REMOVE_SOURCE".equals(mode)) {
                source.fields().forEachRemaining(entry -> {
                    if (!targetNames.contains(entry.getKey()) && !sourceNames.contains(entry.getKey())) {
                        target.set(entry.getKey(), entry.getValue());
                    }
                });
            }
            if (!"REMOVE_TARGET".equals(mode)) {
                for (Mapping mapping : mappings) {
                    JsonNode value = DAGElementUtil.jsonValue(source, mapping.source);
                    Class<?> converter = DAGCore.type(mapping.clsType);
                    if (null != converter) {
                        value = DAGElementUtil.safeJson(DAGCore.convert(converter,
                                DPUtil.toJSON(value, Object.class)));
                    }
                    target.set(mapping.target, DAGElementUtil.safeJson(value));
                }
            }
            return target;
        }).returns(Types.GENERIC(ObjectNode.class));
        return (DataStream<Object>) (DataStream<?>) result;
    }

    static List<Mapping> mappings(JsonNode items) {
        List<Mapping> result = new ArrayList<>();
        if (null == items || !items.isArray()) return result;
        for (JsonNode item : items) {
            String target = item.at("/target").asText("").trim();
            if (DPUtil.empty(target)) continue;
            String source = item.at("/source").asText("").trim();
            String clsType = item.at("/clsType").asText("").trim();
            result.add(new Mapping(target, DPUtil.empty(source) ? target : source, clsType));
        }
        return result;
    }

    static String mode(String value) {
        String mode = DPUtil.empty(value) ? "KEEP_SOURCE" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("KEEP_SOURCE", "REMOVE_SOURCE", "ONLY_TARGET", "REMOVE_TARGET").contains(mode)) {
            throw new IllegalArgumentException("不支持的字段映射模式：" + value);
        }
        return mode;
    }

    static class Mapping implements Serializable {
        final String target;
        final String source;
        final String clsType;

        Mapping(String target, String source, String clsType) {
            this.target = target;
            this.source = source;
            this.clsType = clsType;
        }
    }
}
