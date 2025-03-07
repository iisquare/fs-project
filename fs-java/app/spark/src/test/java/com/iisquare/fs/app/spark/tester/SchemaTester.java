package com.iisquare.fs.app.spark.tester;

import com.iisquare.fs.app.spark.util.SparkUtil;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;

public class SchemaTester implements Serializable {

    @Test
    public void mapTest() {
        SparkSession spark = SparkSession.builder().appName("schema-map").master("local").getOrCreate();
        ExpressionEncoder<Row> encoder = RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("aid", DataTypes.IntegerType, false),
                DataTypes.createStructField("score", DataTypes.IntegerType, false)
        )));
        Dataset<Row> dataset = spark.createDataset(Arrays.asList(
                RowFactory.create(1, 1),
                RowFactory.create(2, 1)
        ), encoder);
        dataset = dataset.map((MapFunction<Row, Row>) row -> {
            Integer score = row.getAs("score");
            return SparkUtil.row(encoder.schema(), row.get(0), score * 10);
        }, encoder);
        dataset.foreachPartition((ForeachPartitionFunction<Row>) iterator -> {
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Integer score = row.getAs("score");
                System.out.printf("[%s] score: %d\n", Thread.currentThread().getName(), score);
            }
        });
        spark.close();
    }

}
