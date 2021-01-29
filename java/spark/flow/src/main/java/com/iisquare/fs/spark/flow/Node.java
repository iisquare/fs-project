package com.iisquare.fs.spark.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.spark.util.SparkUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.io.Serializable;
import java.util.*;

@Setter
@Getter
public abstract class Node implements Serializable {

    protected SparkSession session;
    protected String type; // 环境类型，批处理或流处理

    protected String id; // 当前节点标识
    protected String properties; // 节点原始配置
    protected ObjectNode config; // 节点解析配置
    protected JsonNode flow; // 原始流程图
    protected Set<Node> source = new HashSet<>(); // 上级节点
    protected Set<Node> target = new HashSet<>(); // 下级节点

    protected Integer parallelism; // 当前节点的并行度配置
    protected StructType schema; // 字段类型
    protected Map<String, String> reflect; // 字段映射

    protected boolean disabled = false; // 禁用
    protected boolean isReady = false; // 是否已处理
    protected Dataset<Row> result; // 当前节点执行结果

    public Node() {}

    public JsonNode prepare() throws Exception {
        JsonNode json = DPUtil.parseJSON(properties);
        if (null == json) json = DPUtil.objectNode();
        return this.config = (ObjectNode) json;
    }
    protected abstract StreamNode stream();
    protected abstract BatchNode batch();

    public Object execute() {
        try {
            JsonNode config = prepare();
            switch (type) {
                case "batch":
                    BatchNode batch = batch();
                    if (null == batch) return null;
                    batch.setCurrent(this);
                    this.result = batch.run(config);
                    break;
                case "stream":
                    StreamNode stream = stream();
                    if (null == stream) return null;
                    stream.setCurrent(this);
                    this.result = stream.run(config);
                    break;
                default:
                    this.result = null;
            }
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public Dataset<Row> result() {
        if (null == result || null == reflect || reflect.size() < 1) return result;
        Map<String, Map<String, Object>> schemaResult = SparkUtil.schema(result.schema());
        Map<String, Map<String, Object>> schemaReflect = SparkUtil.schema(this.schema);
        return session.createDataFrame(result.javaRDD().map((Function<Row, Row>) row -> {
            List<Object> data = new ArrayList<>(this.schema.size());
            for (Map.Entry<String, Map<String, Object>> entry : schemaReflect.entrySet()) {
                String name = entry.getKey();
                String reflect = this.reflect.containsKey(name) ? this.reflect.get(name) : name;
                Map<String, Object> fieldResult = schemaResult.get(reflect);
                if (null == fieldResult) {
                    data.add(null);
                    continue;
                }
                int index = (int) fieldResult.get("index");
                Object value = row.get(index);
                Map<String, Object> fieldReflect = entry.getValue();
                DataType dataType = (DataType) fieldReflect.get("dataType");
                if (dataType.sameType((DataType) fieldResult.get("dataType"))) {
                    data.add(value);
                    continue;
                }
                try {
                    if (dataType.sameType(DataTypes.StringType)) {
                        value = DPUtil.parseString(value);
                    } else if (dataType.sameType(DataTypes.BooleanType)) {
                        value = DPUtil.parseBoolean(value);
                    } else if (dataType.sameType(DataTypes.DoubleType)) {
                        value = DPUtil.parseDouble(value);
                    } else if (dataType.sameType(DataTypes.FloatType)) {
                        value = DPUtil.parseFloat(value);
                    } else if (dataType.sameType(DataTypes.IntegerType)) {
                        value = DPUtil.parseInt(value);
                    } else if (dataType.sameType(DataTypes.LongType)) {
                        value = DPUtil.parseLong(value);
                    }
                } catch (Exception e) {
                    throw new Exception("parse field[" + name + "] with reflect["
                            + reflect + "] value [" + value + "] error:" + DPUtil.stringify(row), e);
                }
                data.add(value);
            }
            return RowFactory.create(data.toArray());
        }), this.schema);
    }

}
