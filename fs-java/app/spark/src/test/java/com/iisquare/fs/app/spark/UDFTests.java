package com.iisquare.fs.app.spark;

import com.iisquare.fs.app.spark.udf.DatePartUDF;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.UDFRegistration;
import org.junit.Test;

public class UDFTests {

    @Test
    public void udfTest() {
        SparkSession session = SparkSession.builder().appName("udf-test").master("local").getOrCreate();
        UDFRegistration udf = session.udf();
        udf.register(DatePartUDF.NAME, new DatePartUDF(), DatePartUDF.TYPE);
        Dataset<Row> dataset = session.sql("select date_part('epoch', current_timestamp() - to_timestamp('2022-11-03 00:00:00'))");
        dataset.printSchema();
        dataset.show();
        session.close();
    }

    @Test
    public void implodeTest() {
        SparkSession session = SparkSession.builder().appName("implode-test").master("local").getOrCreate();
        String sql = "select id, array_join(array_agg(name), ',') from values" +
                " (1, 'a')," +
                " (1, 'b')," +
                " (2, 'c')," +
                " (2, 'c')," +
                " (2, 'a')," +
                " (3, 'd')" +
                " as t(id, name) group by id";
        Dataset<Row> dataset = session.sql(sql);
        dataset.show();
        session.close();
    }

}
