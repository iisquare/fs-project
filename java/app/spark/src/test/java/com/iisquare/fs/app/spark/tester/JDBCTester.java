package com.iisquare.fs.app.spark.tester;

import com.iisquare.fs.app.spark.util.ConfigUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Arrays;

public class JDBCTester {

    @Test
    public void upsertTest() {
        SparkConf config = ConfigUtil.spark().setAppName(getClass().getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Dataset<Row> dataset = session.createDataset(Arrays.asList(
                RowFactory.create(1, new Timestamp(System.currentTimeMillis()))
        ), RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("id", DataTypes.IntegerType, false),
                DataTypes.createStructField("t", DataTypes.TimestampType, false)
        ))));
        dataset.show();
        session.close();
    }

}
