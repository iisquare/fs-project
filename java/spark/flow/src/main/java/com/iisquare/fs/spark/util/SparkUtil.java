package com.iisquare.fs.spark.util;

import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.collection.Iterator;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class SparkUtil {

    public static final Map<String, Map<String, Object>> types = new LinkedHashMap<String, Map<String, Object>>(){{
        put(String.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", String.class.getSimpleName());
            put("java", String.class);
            put("spark", DataTypes.StringType);
        }});
        put(Boolean.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Boolean.class.getSimpleName());
            put("java", Boolean.class);
            put("spark", DataTypes.BooleanType);
        }});
        put(Date.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Date.class.getSimpleName());
            put("java", Date.class);
            put("spark", DataTypes.DateType);
        }});
        put(Timestamp.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Timestamp.class.getSimpleName());
            put("java", Timestamp.class);
            put("spark", DataTypes.TimestampType);
        }});
        put(Double.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Double.class.getSimpleName());
            put("java", Double.class);
            put("spark", DataTypes.DoubleType);
        }});
        put(Float.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Float.class.getSimpleName());
            put("java", Float.class);
            put("spark", DataTypes.FloatType);
        }});
        put(Byte.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Byte.class.getSimpleName());
            put("java", Byte.class);
            put("spark", DataTypes.ByteType);
        }});
        put(Integer.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Integer.class.getSimpleName());
            put("java", Integer.class);
            put("spark", DataTypes.IntegerType);
        }});
        put(Long.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Long.class.getSimpleName());
            put("java", Long.class);
            put("spark", DataTypes.LongType);
        }});
        put(Short.class.getName(), new LinkedHashMap<String, Object>() {{
            put("name", Short.class.getSimpleName());
            put("java", Short.class);
            put("spark", DataTypes.ShortType);
        }});
    }};

    public static DataType java2spark(String classname) {
        Map<String, Object> map = types.get(classname);
        if (null == map) return DataTypes.DateType;
        return (DataType) map.get("spark");
    }

    public static Map<String, Map<String, Object>> schema(StructType schema) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (null == schema) return result;
        Iterator<StructField> iterator = schema.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            StructField field = iterator.next();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("index", index++);
            data.put("structField", field);
            data.put("dataType", field.dataType());
            result.put(field.name(), data);
        }
        return result;
    }

}
