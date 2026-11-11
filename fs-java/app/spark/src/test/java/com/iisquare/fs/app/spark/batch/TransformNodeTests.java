package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TransformNodeTests {

    @Test
    public void aggregateQueryTest() {
        assertEquals(
                "SELECT `category`,`status`,count(*) AS total FROM `orders` GROUP BY `category`,`status`",
                AggregateTransformNode.query("`orders`", "category, status", "count(*) AS total"));
    }

    @Test
    public void identifierTest() {
        assertEquals("`order``items`", AggregateTransformNode.identifier("order`items"));
    }

    @Test
    public void convertTransformWithoutRowEncoderTest() {
        SparkSession spark = SparkSession.builder()
                .master("local[1]")
                .appName("convert-transform-test")
                .config("spark.ui.enabled", "false")
                .getOrCreate();
        try {
            StructType schema = DataTypes.createStructType(Arrays.asList(
                    DataTypes.createStructField("id", DataTypes.IntegerType, false),
                    DataTypes.createStructField("name", DataTypes.StringType, false)
            ));
            Dataset<Row> source = spark.createDataFrame(
                    Arrays.asList(RowFactory.create(7, "alice")), schema);
            JsonNode options = DPUtil.parseJSON("{\"mode\":\"ONLY_TARGET\",\"items\":["
                    + "{\"target\":\"user_id\",\"source\":\"id\",\"clsType\":\"java.lang.Integer\"},"
                    + "{\"target\":\"display_name\",\"source\":\"name\",\"clsType\":\"java.lang.String\"},"
                    + "{\"target\":\"missing\",\"source\":\"unknown\",\"clsType\":\"java.lang.String\"}]}");
            Dataset<Row> result = ConvertTransformNode.transform(source, options);
            assertEquals(Arrays.asList("user_id", "display_name", "missing"),
                    Arrays.asList(result.columns()));
            Row row = result.first();
            assertEquals(7, row.getInt(0));
            assertEquals("alice", row.getString(1));
            assertNull(row.get(2));
        } finally {
            spark.stop();
        }
    }
}
