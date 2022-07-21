package com.iisquare.fs.app.spark.tester;

import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.tool.SQLBuilder;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SparkTester implements Serializable {

    @Test
    public void batchTest() {
        SparkConf conf = new SparkConf().setAppName("batch-test").setMaster("local");
        JavaSparkContext context = new JavaSparkContext(conf);
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        JavaRDD<Integer> rdd = context.parallelize(data);
        rdd.foreach(item -> {
            System.out.println(item);
        });
        context.close();
    }

    /**
     * 创建临时Socket服务
     * nc -lk 9999
     */
    @Test
    public void streamTest() throws Exception {
        SparkSession session = SparkSession.builder().appName("stream-test").master("local").getOrCreate();
        Dataset<Row> lines = session.readStream().format("socket")
                .option("host", "localhost").option("port", 9999).load();
        Dataset<String> words = lines.as(Encoders.STRING())
                .flatMap((FlatMapFunction<String, String>) x -> Arrays.asList(x.split(" ")).iterator(), Encoders.STRING());
        Dataset<Row> wordCounts = words.groupBy("value").count();
        try {
            StreamingQuery query = wordCounts.writeStream().outputMode("complete").format("console").start();
            query.awaitTermination();
        } finally {
            session.close();
        }
    }

    @Test
    public void sqlTest() {
        SparkSession session = SparkSession.builder().appName("sql-test").master("local").getOrCreate();
        String url = "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Properties properties = new Properties();
        properties.put("driver", "com.mysql.jdbc.Driver");
        properties.put("user", "root");
        properties.put("password", "admin888");
        Dataset<Row> dataset = session.read().jdbc(url, "t_memory", properties);
        dataset.show();
        dataset = dataset.map((MapFunction<Row, Row>) row ->
                RowFactory.create(row.get(0), row.get(1), row.getDecimal(2).add(BigDecimal.valueOf(1))), dataset.encoder());
        dataset.show();
        StructType schema = dataset.schema();
        dataset.foreachPartition((ForeachPartitionFunction<Row>) t -> {
            List<Map<String, Object>> data = SparkUtil.row2list(schema, t);
            String sql = SQLBuilder.build("t_memory").batchInsert(data, true, "score");
            Connection connection = DriverManager.getConnection(url, "root", "admin888");
            Statement statement = connection.createStatement();
            int result = statement.executeUpdate(sql);
            System.out.println(result);
            FileUtil.close(statement, connection);
        });
        session.close();
    }

    @Test
    public void multiTest() {
        SparkSession session = SparkSession.builder().appName("sql-test").master("local").getOrCreate();
        String url = "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Properties properties = new Properties();
        properties.put("driver", "com.mysql.jdbc.Driver");
        properties.put("user", "root");
        properties.put("password", "admin888");
        Dataset<Row> dataset = session.read().jdbc(url, "fs_member_resource", properties);
        dataset.map(new MapFunction<Row, Row>() {
            @Override
            public Row call(Row row) throws Exception {
                return RowFactory.create(row.getString(4));
            }
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("module", DataTypes.StringType, false)
        )))).show();
        dataset.map(new MapFunction<Row, Row>() {
            @Override
            public Row call(Row row) throws Exception {
                return RowFactory.create(row.getString(5), row.getString(6));
            }
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("controller", DataTypes.StringType, false),
                DataTypes.createStructField("action", DataTypes.StringType, false)
        )))).show();
        session.close();
    }

}
