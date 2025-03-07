package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkCore;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.*;

import java.util.*;

public class ConvertTransformNode extends AbstractConvertTransform {
    @Override
    public Object process() {
        Dataset<Row> dataset = SparkUtil.union(Dataset.class, sources);
        StructType schema = dataset.schema();
        Map<String, StructField> sources = SparkUtil.field(schema); // 源字段类型
        String mode = options.at("/mode").asText(""); // 处理方式
        Map<String, Integer> reflect = SparkUtil.index(schema); // 目标字段对应源字段索引，为null时源字段不存在
        Map<String, Class<?>> convert = new LinkedHashMap<>(); // 目标字段的转换类型，为null时保持源字段类型
        Map<String, StructField> targets = new LinkedHashMap<>(); // 目标字段类型，保留的源字段在sources变量中
        Iterator<JsonNode> iterator = options.at("/items").iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String target = item.at("/target").asText("");
            if (DPUtil.empty(target)) continue;
            String source = item.at("/source").asText("");
            if (DPUtil.empty(source)) source = target;
            String clsType = item.at("/clsType").asText("");
            StructField field = sources.get(source);
            reflect.put(target, null == field ? null : schema.fieldIndex(source));
            convert.put(target, DAGCore.type(clsType));
            DataType type = SparkCore.type(clsType);
            if (null == type) {
                type = null == field ? DataTypes.NullType : field.dataType();
            }
            if (null != field && field.dataType().equals(type)) {
                convert.put(target, null); // 目标字段与引用字段类型一致时不执行类型转换
            }
            targets.put(target, StructField.apply(target, type, true, Metadata.empty()));
        }
        switch (mode) {
            case "KEEP_SOURCE": // 保留引用字段
                for (Map.Entry<String, StructField> entry : targets.entrySet()) {
                    sources.remove(entry.getKey()); // 移除与目标字段一致的引用字段
                }
                break;
            case "REMOVE_SOURCE": // 删除引用字段
                for (Map.Entry<String, Integer> entry : reflect.entrySet()) {
                    sources.remove(entry.getKey()); // 移除引用字段
                }
                break;
            case "ONLY_TARGET": // 仅保留目标字段
                sources.clear();
                break;
            case "REMOVE_TARGET": // 仅移除目标字段
                return dataset.drop(targets.keySet().toArray(new String[0])); // 删除字段后直接返回
        }
        List<StructField> list = new ArrayList<>();
        list.addAll(targets.values());
        list.addAll(sources.values());
        return dataset.map((MapFunction<Row, Row>) row -> {
            List<Object> data = new ArrayList<>();
            for (Map.Entry<String, StructField> entry : targets.entrySet()) {
                String key = entry.getKey();
                Integer index = reflect.get(key);
                if (null == index) {
                    data.add(null); // 引用字段不存在
                    continue;
                }
                Object value = row.get(index);
                Class<?> cls = convert.get(key);
                if (null != cls) { // 字段类型不一致，执行转换
                    value = DAGCore.convert(cls, value);
                }
                data.add(value);
            }
            for (Map.Entry<String, StructField> entry : sources.entrySet()) {
                data.add(row.get(reflect.get(entry.getKey())));
            }
            return SparkUtil.row(data);
        }, RowEncoder.apply(DataTypes.createStructType(list.toArray(new StructField[0]))));
    }

}
