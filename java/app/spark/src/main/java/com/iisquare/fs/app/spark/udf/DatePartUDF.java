package com.iisquare.fs.app.spark.udf;

import org.apache.spark.sql.api.java.UDF2;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;

import java.sql.Timestamp;
import java.time.Duration;

public class DatePartUDF implements UDF2<String, Object, Double> {

    public static final String NAME = "date_part"; // 覆盖内置函数
    public static final DataType TYPE = DataTypes.DoubleType;

    @Override
    public Double call(String field, Object source) throws Exception {
        if (null == source) return null;
        switch (field.toUpperCase()) {
            case "EPOCH":
                if (source instanceof Timestamp) {
                    return (double) ((Timestamp) source).getTime() / 1000;
                } else if (source instanceof Duration) {
                    return (double) ((Duration) source).getSeconds();
                } else {
                    throw new Exception(String.format("udf [%s] not support field [%s] of type [%s]", NAME, field, source.getClass().getName()));
                }
            default:
                throw new Exception(String.format("udf [%s] not support field [%s]", NAME, field));
        }
    }
}
