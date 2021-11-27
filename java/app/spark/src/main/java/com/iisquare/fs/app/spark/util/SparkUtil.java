package com.iisquare.fs.app.spark.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGNode;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.*;

public class SparkUtil {

    public static Dataset<Row> agg(RelationalGroupedDataset dataset, Collection<Column> columns) {
        if (null == columns || columns.size() < 1) return null;
        Iterator<Column> iterator = columns.iterator();
        Column column = iterator.next();
        List<Column> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return dataset.agg(column, list.toArray(new Column[0]));
    }

    public static <T> Dataset<T> union(Dataset<T>... ds) {
        if (ds.length < 1) return null;
        Dataset<T> dataset = ds[0];
        for (int i = 1; i < ds.length; i++) {
            dataset = dataset.union(ds[i]);
        }
        return dataset;
    }

    public static <T> T union(Class<T> classType, Set<DAGNode> nodes) {
        if (null == nodes || 0 == nodes.size()) return null;
        Iterator<DAGNode> iterator = nodes.iterator();
        T result = iterator.next().result(classType);
        while (iterator.hasNext()) {
            if (result instanceof Dataset) {
                result = (T) ((Dataset) result).union(iterator.next().result(Dataset.class));
                continue;
            }
            return null;
        }
        return result;
    }

    public static Map<String, StructField> field(StructType schema) {
        Map<String, StructField> result = new LinkedHashMap<>();
        for (StructField field : schema.fields()) {
            result.put(field.name(), field);
        }
        return result;
    }

    public static Map<String, Integer> index(StructType schema) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (StructField field : schema.fields()) {
            String name = field.name();
            result.put(name, schema.fieldIndex(name));
        }
        return result;
    }

    public static Row row(Collection<Object> data) {
        return RowFactory.create(data.toArray(new Object[0]));
    }

    public static ArrayNode dataset2json(Dataset<Row> dataset) {
        List<Row> list = dataset.collectAsList();
        String[] columns = dataset.columns();
        ArrayNode result = DPUtil.arrayNode();
        for (Row row : list) {
            ObjectNode item = result.addObject();
            for (int i = 0; i < columns.length; i++) {
                item.replace(columns[i], DPUtil.toJSON(row.get(i)));
            }
        }
        return result;
    }

    public static ObjectNode row2json(StructType schema, Row row) {
        StructField[] fields = schema.fields();
        ObjectNode result = DPUtil.objectNode();
        for (int i = 0; i < fields.length; i++) {
            result.replace(fields[i].name(), DPUtil.toJSON(row.get(i)));
        }
        return result;
    }

    public static Row json2row(JsonNode json) {
        List<Object> data = new ArrayList<>();
        Iterator<JsonNode> iterator = json.iterator();
        while (iterator.hasNext()) {
            data.add(DPUtil.toJSON(iterator.next(), Object.class));
        }
        return row(data);
    }

}
