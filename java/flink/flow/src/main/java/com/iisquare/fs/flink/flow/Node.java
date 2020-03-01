package com.iisquare.fs.flink.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
public abstract class Node implements Serializable {

    protected JsonNode flow; // 原始流程图
    protected String config; // 当前节点配置
    protected String environment; // 环境类型
    protected boolean isReady = false; // 是否已处理
    protected Set<Node> source = new HashSet<>(); // 上级节点
    protected Set<Node> target = new HashSet<>(); // 下级节点
    protected String id; // 当前节点主键
    protected RowTypeInfo typeInfo; // 字段类型
    protected Map<String, String> fieldReflect; // 字段映射
    protected BatchNode batch;
    protected StreamNode stream;
    protected ExecutionEnvironment batchEnvironment;
    protected StreamExecutionEnvironment streamEnvironment;
    protected Integer parallelism; // 当前节点的并行度配置
    protected ObjectNode properties; // 传递实例变量

    public Node() {}

    protected MapFunction fieldReflect() {
        String[] fieldNames = typeInfo.getFieldNames();
        TypeInformation<?>[] fieldTypes = typeInfo.getFieldTypes();
        Map<String, String> fieldReflect = this.fieldReflect;
        return new MapFunction<Map<String, Object>, Map<String, Object>>() {
            @Override
            public Map<String, Object> map(Map<String, Object> record) throws Exception {
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < fieldNames.length; i++) {
                    String field = fieldNames[i];
                    String reflect = fieldReflect.containsKey(field) ? fieldReflect.get(field) : field;
                    Object value = record.get(reflect);
                    if (null == value) {
                        map.put(field, value);
                        continue;
                    }
                    Class typeClass = fieldTypes[i].getTypeClass();
                    if (typeClass.isInstance(value)) {
                        map.put(field, value);
                        continue;
                    }
                    // 根据常用类型转换，设置判断优先级
                    try {
                        if (typeClass.equals(String.class)) {
                            value = DPUtil.parseString(value);
                        } else if (typeClass.equals(Integer.class)) {
                            value = DPUtil.parseInt(value);
                        } else if (typeClass.equals(Long.class)) {
                            value = DPUtil.parseLong(value);
                        } else if (typeClass.equals(Double.class)) {
                            value = DPUtil.parseDouble(value);
                        } else if (typeClass.equals(Float.class)) {
                            value = DPUtil.parseFloat(value);
                        } else if (typeClass.equals(Boolean.class)) {
                            value = DPUtil.parseBoolean(value);
                        }
                    } catch (Exception e) {
                        throw new Exception("parse field[" + field + "] with reflect["
                            + reflect + "] value [" + value + "] error:" + DPUtil.stringify(record), e);
                    }
                    map.put(field, value);
                }
                return map;
            }
        };
    }

    public abstract JsonNode run() throws Exception;
    protected abstract StreamNode stream();
    protected abstract BatchNode batch();

    public Object process() {
        batch = batch();
        if(null != batch) {
            batch.setCurrent(this);
            batch.setEnvironment(batchEnvironment);
        }
        stream = stream();
        if(null != stream) {
            stream.setCurrent(this);
            stream.setEnvironment(streamEnvironment);
        }
        try {
            JsonNode config = run();
            switch (environment) {
                case "batch":
                    batch.setResult(batch.run(config));
                    break;
                case "stream":
                    stream.setResult(stream.run(config));
                    break;
            }
            return null;
        } catch (Exception e) {
            return e;
        }
    }

}
