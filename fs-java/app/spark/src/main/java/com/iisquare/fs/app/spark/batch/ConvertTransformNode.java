package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkCore;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConvertTransformNode extends AbstractConvertTransform<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() {
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        if (null == dataset) throw new IllegalArgumentException("字段映射节点缺少上游数据");
        return transform(dataset, options);
    }

    /**
     * 使用原生 Column 表达式完成字段映射。
     *
     * 不通过 Dataset.map + RowEncoder 重建整行，避免运行环境对 Encoder 实现的限制，
     * 同时保留目标字段顺序、来源类型和显式 cast。
     */
    static Dataset<Row> transform(Dataset<Row> dataset, JsonNode options) {
        StructType schema = dataset.schema();
        Map<String, StructField> allSources = SparkUtil.field(schema);
        Map<String, StructField> keptSources = new LinkedHashMap<>(allSources);
        Map<String, Mapping> mappings = new LinkedHashMap<>();
        Set<String> referencedSources = new LinkedHashSet<>();
        Iterator<JsonNode> iterator = options.at("/items").iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String target = item.at("/target").asText("");
            if (DPUtil.empty(target)) continue;
            String source = item.at("/source").asText("");
            if (DPUtil.empty(source)) source = target;
            String clsType = item.at("/clsType").asText("");
            StructField sourceField = allSources.get(source);
            DataType type = SparkCore.type(clsType);
            if (null == type) {
                type = null == sourceField ? DataTypes.StringType : sourceField.dataType();
            }
            referencedSources.add(source);
            mappings.put(target, new Mapping(target, source, sourceField, type));
        }
        if (mappings.isEmpty()) return dataset;
        String mode = options.at("/mode").asText("");
        switch (mode) {
            case "KEEP_SOURCE":
                mappings.keySet().forEach(keptSources::remove);
                break;
            case "REMOVE_SOURCE":
                keptSources.keySet().removeIf(name ->
                        referencedSources.contains(name) || mappings.containsKey(name));
                break;
            case "ONLY_TARGET":
                keptSources.clear();
                break;
            case "REMOVE_TARGET":
                return dataset.drop(mappings.keySet().toArray(new String[0]));
            default:
                break;
        }
        List<Column> columns = new ArrayList<>();
        for (Mapping mapping : mappings.values()) {
            Column column = null == mapping.sourceField
                    ? functions.lit(null).cast(mapping.type)
                    : functions.col(quote(mapping.source));
            if (null != mapping.sourceField && !mapping.sourceField.dataType().equals(mapping.type)) {
                column = column.cast(mapping.type);
            }
            columns.add(column.as(mapping.target));
        }
        for (String source : keptSources.keySet()) columns.add(functions.col(quote(source)));
        return dataset.select(columns.toArray(new Column[0]));
    }

    private static String quote(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    static class Mapping {
        final String target;
        final String source;
        final StructField sourceField;
        final DataType type;

        Mapping(String target, String source, StructField sourceField, DataType type) {
            this.target = target;
            this.source = source;
            this.sourceField = sourceField;
            this.type = type;
        }
    }
}
