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
import org.apache.spark.sql.catalog.Catalog;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
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
import java.util.*;

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
    public void kafkaTest() throws Exception {
        SparkSession session = SparkSession.builder().appName("kafka-test").master("local").getOrCreate();
        Map<String, String> options = new LinkedHashMap<>();
        options.put("kafka.bootstrap.servers", "kafka:9092");
        options.put("subscribe", "fs_test");
        options.put("startingOffsets", "earliest");
        options.put("endingOffsets", "latest");
        Dataset<Row> dataset = session.read().format("kafka").options(options).load();
        dataset.show();
        session.close();
    }

    @Test
    public void sqlTest() {
        SparkSession session = SparkSession.builder().appName("sql-test").master("local").getOrCreate();
        String url = "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Properties properties = new Properties();
        properties.put("driver", "com.mysql.cj.jdbc.Driver");
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
            String sql = SQLBuilder.build("t_memory").batchInsert(data, "score");
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
        properties.put("driver", "com.mysql.cj.jdbc.Driver");
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

    @Test
    public void memoryTest() {
        SparkSession spark = SparkSession.builder().appName("write-test").master("local").getOrCreate();
        ExpressionEncoder<Row> encoder = RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("t", DataTypes.StringType, false),
                DataTypes.createStructField("j", DataTypes.IntegerType, false),
                DataTypes.createStructField("score", DataTypes.DoubleType, false),
                DataTypes.createStructField("time", DataTypes.LongType, false)
        )));
        for (int i = 0; i < 10000; i++) {
            SparkSession session = spark.newSession();
            String table = String.format("t_%d", i);
            List<Row> data = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                data.add(RowFactory.create(table, j, Math.random(), System.currentTimeMillis()));
            }
            Dataset<Row> dataset = session.createDataset(data, encoder);
            dataset.createOrReplaceTempView(table);
            String sql = String.format("select t, max(score) from %s group by t", table);
            dataset = session.sql(sql);
            session.catalog().dropTempView(table);
            dataset.show();
        }
        spark.close();
    }

    @Test
    public void cacheTest() {
        SparkSession spark = SparkSession.builder().appName("cache-test").master("local").getOrCreate();
        String url = "jdbc:mysql://127.0.0.1:3306/fs_test?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
        Properties properties = new Properties();
        properties.put("driver", "com.mysql.cj.jdbc.Driver");
        properties.put("user", "root");
        properties.put("password", "admin888");
        Catalog catalog = spark.catalog();
        String table = String.format("t_cache");
        for (int i = 0; i < 6; i++) {
            String where = String.format("aid in (%d)", i % 4);
            Dataset<Row> dataset = spark.read().jdbc(url, "ta", properties).where(where);
            if (catalog.tableExists(table)) {
                Dataset<Row> other = spark.table(table);
                dataset = dataset.union(other).dropDuplicates("aid", "bid");
                catalog.uncacheTable(table);
            }
            dataset.createOrReplaceTempView(table);
            catalog.cacheTable(table);
        }
        spark.sql(String.format("select * from %s", table)).show();
        catalog.uncacheTable(table);
        catalog.dropTempView(table);
        spark.close();
    }

    @Test
    public void explainTest() {
        SparkSession spark = SparkSession.builder().appName("explain-test").master("local").getOrCreate();
        spark.createDataset(Arrays.asList(
                RowFactory.create(1, 1),
                RowFactory.create(2, 1)
        ), RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("aid", DataTypes.IntegerType, false),
                DataTypes.createStructField("score", DataTypes.IntegerType, false)
        )))).createOrReplaceTempView("t_a");
        spark.createDataset(Arrays.asList(
                RowFactory.create(1, 1),
                RowFactory.create(2, 2),
                RowFactory.create(3, 0),
                RowFactory.create(4, 1)
        ), RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("bid", DataTypes.IntegerType, false),
                DataTypes.createStructField("aid", DataTypes.IntegerType, false)
        )))).createOrReplaceTempView("t_b");
        spark.createDataset(Arrays.asList(
                RowFactory.create(1, 1, "2021"),
                RowFactory.create(2, 2, "2021"),
                RowFactory.create(3, 3, "2022")
        ), RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("cid", DataTypes.IntegerType, false),
                DataTypes.createStructField("bid", DataTypes.IntegerType, false),
                DataTypes.createStructField("time", DataTypes.StringType, false)
        )))).createOrReplaceTempView("t_c");
        List<String> list = Arrays.asList(
                "select a.aid as aid, b.bid as bid, count(*) as ct from t_a as a join t_b as b on a.aid = b.aid group by a.aid, b.bid",
                "select a.aid as aid, b.bid as bid, count(*) as ct from t_a as a join t_b as b on a.aid = b.aid where a.aid = 1 group by a.aid, b.bid",
                "select a.aid as aid, b.bid as bid, count(*) as ct from t_a as a join t_b as b on a.aid = b.aid where b.bid = 1 group by a.aid, b.bid",
                "select a.aid as aid, b.bid as bid, count(*) as ct, max(time) as time " +
                    "from t_a as a join t_b as b on a.aid = b.aid join t_c as c on c.bid = b.bid group by a.aid, b.bid ",
                "select a.aid as aid, b.bid as bid, count(*) as ct " +
                        "from t_a as a join t_b as b on a.aid = b.aid where b.bid in (select bid from t_c) group by a.aid, b.bid",
                "select a.aid as aid, b.bid as bid, count(*) as ct " +
                        "from t_a as a join (" +
                            "select * from t_b where bid in (select bid from t_c)" +
                        ") as b on a.aid = b.aid group by a.aid, b.bid"
        );
        for (String sql : list) {
            System.out.println("SQL: " + sql);
            spark.sql(sql).explain(true);
        }
        spark.close();
    }

}
