package com.iisquare.fs.app.spark.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.udf.NullIfUDF;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGNode;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.collection.Seq;

import java.util.*;

public class SparkUtil {

    public static Dataset<Row> agg(RelationalGroupedDataset dataset, Collection<Column> columns) {
        if (null == columns || columns.isEmpty()) return null;
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
        if (null == nodes || nodes.isEmpty()) return null;
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

    /**
     * 指定StructType创建Row
     * 通过RowFactory.create()方式创建的Row不能使用字段名称获取值
     * java.lang.UnsupportedOperationException: fieldIndex on a Row without schema is undefined.
     */
    public static Row row(StructType struct, Object ... values) {
        return new GenericRowWithSchema(values, struct);
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

    public static JsonNode row2json(Row row) {
        return DPUtil.parseJSON(row.json()); // Row自身需要包含Schema
    }

    public static ArrayNode seq2json(DataType elementType, Seq<Object> seq) {
        ArrayNode array = DPUtil.arrayNode();
        int size = seq.size();
        for (int i = 0; i < size; i++) {
            switch (elementType.typeName()) {
                case "struct":
                    array.add(row2json((StructType) elementType, (Row) seq.apply(i)));
                    break;
                case "array":
                    array.add(seq2json(((ArrayType) elementType).elementType(), (Seq<Object>) seq.apply(i)));
                    break;
                default:
                    array.add(DPUtil.toJSON(seq.apply(i)));
            }
        }
        return array;
    }

    public static ObjectNode row2json(StructType schema, Row row) {
        StructField[] fields = schema.fields();
        ObjectNode result = DPUtil.objectNode();
        for (int i = 0; i < fields.length; i++) {
            StructField field = fields[i];
            String name = field.name();
            DataType type = field.dataType();
            switch (type.typeName()) {
                case "struct":
                    result.replace(name, row2json((StructType) type, row.getStruct(i)));
                    break;
                case "array":
                    result.replace(name, seq2json(((ArrayType) type).elementType(), row.getSeq(i)));
                    break;
                default:
                    result.replace(name, DPUtil.toJSON(row.get(i)));
            }
        }
        return result;
    }

    public static List<Object> seq2list(DataType elementType, Seq<Object> seq) {
        List<Object> array = new ArrayList<>();
        int size = seq.size();
        for (int i = 0; i < size; i++) {
            switch (elementType.typeName()) {
                case "struct":
                    array.add(row2map((StructType) elementType, (Row) seq.apply(i)));
                    break;
                case "array":
                    array.add(seq2list(((ArrayType) elementType).elementType(), (Seq<Object>) seq.apply(i)));
                    break;
                default:
                    array.add(seq.apply(i));
            }
        }
        return array;
    }

    public static Map<String, Object> row2map(StructType schema, Row row) {
        StructField[] fields = schema.fields();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < fields.length; i++) {
            StructField field = fields[i];
            String name = field.name();
            DataType type = field.dataType();
            switch (type.typeName()) {
                case "struct":
                    result.put(name, row2map((StructType) type, row.getStruct(i)));
                    break;
                case "array":
                    result.put(name, seq2list(((ArrayType) type).elementType(), row.getSeq(i)));
                    break;
                default:
                    result.put(name, row.get(i));
            }
        }
        return result;
    }

    /**
     * 将Row迭代器转换为List数组
     */
    public static List<Map<String, Object>> row2list(StructType schema, Iterator<Row> iterator) {
        List<Map<String, Object>> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(row2map(schema, iterator.next()));
        }
        return result;
    }

    /**
     * 根据DataType将JSON转换为Seq序列
     */
    public static Seq<Object> json2seq(DataType elementType, JsonNode json) {
        List<Object> array = new ArrayList<>();
        for (JsonNode item : json) {
            switch (elementType.typeName()) {
                case "struct":
                    array.add(json2row((StructType) elementType, item));
                    break;
                case "array":
                    array.add(json2seq(((ArrayType) elementType).elementType(), item));
                    break;
                default:
                    array.add(DPUtil.toJSON(item, Object.class));
            }
        }
        return ScalaUtil.seq(array);
    }

    /**
     * 根据Schema将JSON转换为Row
     */
    public static Row json2row(StructType schema, JsonNode json) {
        StructField[] fields = schema.fields();
        Map<String, Object> result = new LinkedHashMap<>();
        for (StructField field : fields) {
            String name = field.name();
            if (!json.has(name)) {
                result.put(name, null);
                continue;
            }
            JsonNode value = json.at("/" + name);
            DataType type = field.dataType();
            switch (type.typeName()) {
                case "struct":
                    result.put(name, json2row((StructType) type, value));
                    break;
                case "array":
                    result.put(name, json2seq(((ArrayType) type).elementType(), value));
                    break;
                default:
                    result.put(name, DPUtil.toJSON(value, Object.class));
            }
        }
        return row(schema, result.values().toArray(new Object[0]));
    }

    public static SparkSession udf(SparkSession session) {
        UDFRegistration udf = session.udf();
        udf.register(NullIfUDF.NAME, new NullIfUDF(), NullIfUDF.TYPE);
        return session;
    }

}
