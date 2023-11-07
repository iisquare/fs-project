package com.iisquare.fs.app.spark.core;

import com.iisquare.fs.base.dag.DAGCore;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;

import java.util.Date;
import java.util.Map;

public class SparkCore {

    public static final String TYPE_FIELD = "spark";
    public static Map<String, Map<String, Object>> clsTypes = DAGCore.clsTypes;

    static {
        clsTypes.get(Boolean.class.getName()).put(TYPE_FIELD, DataTypes.BooleanType);
        clsTypes.get(Byte.class.getName()).put(TYPE_FIELD, DataTypes.ByteType);
        clsTypes.get(Double.class.getName()).put(TYPE_FIELD, DataTypes.DoubleType);
        clsTypes.get(Float.class.getName()).put(TYPE_FIELD, DataTypes.FloatType);
        clsTypes.get(Integer.class.getName()).put(TYPE_FIELD, DataTypes.IntegerType);
        clsTypes.get(Long.class.getName()).put(TYPE_FIELD, DataTypes.LongType);
        clsTypes.get(Short.class.getName()).put(TYPE_FIELD, DataTypes.ShortType);
        clsTypes.get(String.class.getName()).put(TYPE_FIELD, DataTypes.StringType);
        clsTypes.get(Date.class.getName()).put(TYPE_FIELD, DataTypes.DateType);
        clsTypes.get(java.sql.Date.class.getName()).put(TYPE_FIELD, DataTypes.DateType);
        clsTypes.get(java.sql.Timestamp.class.getName()).put(TYPE_FIELD, DataTypes.TimestampType);
    }

    public static DataType type(String type) {
        if (!clsTypes.containsKey(type)) return null;
        return (DataType) clsTypes.get(type).get(TYPE_FIELD);
    }

}
