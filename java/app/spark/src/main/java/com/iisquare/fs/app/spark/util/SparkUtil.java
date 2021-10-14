package com.iisquare.fs.app.spark.util;

import com.iisquare.fs.base.dag.core.DAGNode;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.*;

public class SparkUtil {

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

}
