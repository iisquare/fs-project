package com.iisquare.fs.app.spark;

import com.iisquare.fs.app.spark.util.ScalaUtil;
import com.iisquare.fs.app.spark.util.SparkUtil;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.Test;

import java.util.Arrays;

public class JsonTests {

    @Test
    public void rowTest() {
        StructType schema = DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("a1", DataTypes.IntegerType, true),
                DataTypes.createStructField("a2", DataTypes.StringType, true),
                DataTypes.createStructField("b", ArrayType.apply(DataTypes.IntegerType), true),
                DataTypes.createStructField("c", DataTypes.createStructType(Arrays.asList(
                        DataTypes.createStructField("c1", DataTypes.IntegerType, true),
                        DataTypes.createStructField("c2", DataTypes.StringType, true),
                        DataTypes.createStructField("d", ArrayType.apply(
                                ArrayType.apply(DataTypes.createStructType(Arrays.asList(
                                        DataTypes.createStructField("d1", DataTypes.IntegerType, true),
                                        DataTypes.createStructField("d2", DataTypes.StringType, true)
                                )))
                        ), true)
                )), true)
        ));
        Row row = RowFactory.create(
                1, // a1
                "2", // a2
                ScalaUtil.seq(Arrays.asList(1, 2, 3)), // b
                RowFactory.create( // c
                        1, // c1
                        "2", // c2
                        ScalaUtil.seq( // d
                                ScalaUtil.seq(RowFactory.create(1, "2")),
                                ScalaUtil.seq(RowFactory.create(3, "4"), RowFactory.create(5, "6"))
                        )
                )
        );
        System.out.println(SparkUtil.row2json(schema, row));
        System.out.println(SparkUtil.row2map(schema, row));
    }

}
