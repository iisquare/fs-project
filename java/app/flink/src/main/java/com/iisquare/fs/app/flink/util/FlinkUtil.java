package com.iisquare.fs.app.flink.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.core.FlinkCore;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGNode;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;
import org.apache.hudi.util.HoodiePipeline;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FlinkUtil {

    public static ObjectNode client(JobClient client) {
        ObjectNode result = DPUtil.objectNode();
        result.put("JobId", client.getJobID().toString());
        CompletableFuture<JobExecutionResult> jobExecutionResult = client.getJobExecutionResult();
        result.put("isDone", jobExecutionResult.isDone());
        result.put("isCancelled", jobExecutionResult.isCancelled());
        return result;
    }

    public static <T> T union(Class<T> classType, Set<DAGNode> nodes) {
        if (null == nodes || 0 == nodes.size()) return null;
        Iterator<DAGNode> iterator = nodes.iterator();
        T result = iterator.next().result(classType);
        while (iterator.hasNext()) {
            if (result instanceof DataSet) {
                result = (T) ((DataSet) result).union(iterator.next().result(DataSet.class));
                continue;
            }
            if (result instanceof DataStream) {
                result = (T) ((DataStream) result).union(iterator.next().result(DataStream.class));
                continue;
            }
            return null;
        }
        return result;
    }

    public static ObjectNode row2json(RowTypeInfo typeInfo, Row row) {
        String[] fields = typeInfo.getFieldNames();
        ObjectNode result = DPUtil.objectNode();
        for (int i = 0; i < fields.length; i++) {
            result.replace(fields[i], DPUtil.toJSON(row.getField(i)));
        }
        return result;
    }

    public static Row json2row(JsonNode json) {
        Row row = new Row(json.size());
        Iterator<JsonNode> iterator = json.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            row.setField(i++, DPUtil.toJSON(iterator.next(), Object.class));
        }
        return row;
    }

    public static RowTypeInfo type(JsonNode json) {
        LinkedHashMap<String, TypeInformation<?>> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        JSON: while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            Class<?> cls = DPUtil.toJSON(entry.getValue(), Object.class).getClass();
            for (Map.Entry<String, Map<String, Object>> types : FlinkCore.clsTypes.entrySet()) {
                Map<String, Object> type = types.getValue();
                if (cls.equals(type.get("cls"))) {
                    result.put(entry.getKey(), (TypeInformation<?>) type.get(FlinkCore.TYPE_FIELD));
                    continue JSON;
                }
            }
            result.put(entry.getKey(), TypeInformation.of(Object.class));
        }
        return new RowTypeInfo(result.values().toArray(new TypeInformation[0]), result.keySet().toArray(new String[0]));
    }

    public static Row row(Object... items) {
        Row row = new Row(items.length);
        for (int i = 0; i < items.length; i++) {
            row.setField(i, items[i]);
        }
        return row;
    }

    public static Map<String, DataType> columns(HoodiePipeline.Builder pipeline) {
        Map<String, DataType> result = new LinkedHashMap<>();
        List<Column> columns = pipeline.getTableDescriptor().getResolvedCatalogTable().getResolvedSchema().getColumns();
        for (Column column : columns) {
            result.put(column.getName(), column.getDataType());
        }
        return result;
    }

    public static Object[] row(Map<String, DataType> columns, JsonNode data) {
        List<Object> result = new ArrayList<>();
        for (Map.Entry<String, DataType> entry : columns.entrySet()) {
            result.add(row(entry.getValue(), DPUtil.toJSON(data.at("/" + entry.getKey()), Object.class)));
        }
        return result.toArray(new Object[0]);
    }

    public static Object row(DataType type, Object obj) {
        if (null == obj) return null;
        Class<?> cls = type.getConversionClass();
        if (String.class.equals(cls)) {
            return StringData.fromString(obj.toString());
        }
        if (Integer.class.equals(cls)) {
            return DPUtil.parseInt(obj);
        }
        if (Long.class.equals(cls)) {
            return DPUtil.parseLong(obj);
        }
        return obj;
    }

}
