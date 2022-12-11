package com.iisquare.fs.app.spark.udf;

import org.apache.spark.sql.api.java.UDF2;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;

public class NullIfUDF implements UDF2<Object, String, Integer> {

    public static final String NAME = "nullif";
    public static final DataType TYPE = DataTypes.IntegerType;

    @Override
    public Integer call(Object o, String s) throws Exception {
        if (o == s) {
            return null;
        } else {
            return Integer.parseInt(o.toString());
        }
    }
}
