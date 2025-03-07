package com.iisquare.fs.app.spark.tester;

import com.iisquare.fs.app.spark.mongo.FSMongoTableProvider;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.PropertiesUtil;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCursor;
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.config.ReadConfig;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.bson.Document;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

public class MongoTester implements Serializable {

    public static final String INPUT_PREFIX = "spark.mongodb.read.";
    public static final String OUTPUT_PREFIX = "spark.mongodb.write.";
    public static final String INPUT3_PREFIX = "spark.mongodb.input.";

    public String ddl(String database, String collection) {
        SparkSession session = SparkSession.builder().appName("ddl-test").master("local").getOrCreate();
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://root:admin888@127.0.0.1:27017";
        config.put(INPUT_PREFIX + "connection.uri", uri);
        config.put(INPUT_PREFIX + "database", database);
        config.put(INPUT_PREFIX + "collection", collection);
        config.put(INPUT_PREFIX + "aggregation.pipeline", "{ $limit: 1000 }");
        Encoder<Row> encoder = session.read().format("mongodb").options(config).load().encoder();
        return encoder.schema().toDDL();
    }

    @Test
    public void encodeTest() {
        MongoClient client = MongoClients.create("mongodb://root:admin888@127.0.0.1:27017/");
        String database = "fs_project";
        MongoCursor<String> iterator = client.getDatabase(database).listCollectionNames().iterator();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNext()) {
            String collection = iterator.next();
            System.out.println(String.format("ddl %s.%s", database, collection));
            // 当集合内无数据时，依然无法获取到结构信息，可适当补充测试数据用于静态化
            sb.append(String.format("\n%s.%s=%s", database, collection, ddl(database, collection)));
        }
        System.out.println(sb.toString());
        FileUtil.close(client);
    }

    /**
     * 无论是aggregation或是find，当查询无结果时将无法获取schema结构
     * 若直接采用查询结果注册临时表，可能导致后续sql查询缺失表字段名称
     * 此时推荐在开发环境导出结果数据的schema，并在初始化作业时导入到变量中
     * 采用固定结构作为aggregation的返回结构，用于后续的sql注册查询
     */
    @Test
    public void decodeTest() throws AnalysisException {
        Properties properties = PropertiesUtil.load(getClass().getClassLoader(), "ddl.properties");
        SparkSession session = SparkSession.builder().appName("sql-test").master("local").getOrCreate();
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://root:admin888@127.0.0.1:27017";
        String database = "fs_project";
        String collection = "fs_test";
        config.put(INPUT_PREFIX + "connection.uri", uri);
        config.put(INPUT_PREFIX + "database", database);
        config.put(INPUT_PREFIX + "collection", collection);

        String pipeline = "{ $match: { i : { $lt : 0 } } }";
        config.put(INPUT_PREFIX + "aggregation.pipeline", pipeline);
        Dataset<Row> dataset = session.read().format("mongodb").options(config).load();

        String ddl = properties.getProperty(database + "." + collection);

        if (!DPUtil.empty(ddl)) {
            dataset.printSchema();
            ExpressionEncoder<Row> encoder = RowEncoder.apply(StructType.fromDDL(ddl));
            dataset = session.createDataset(dataset.rdd(), encoder);
        }

        dataset.createTempView("test");
        session.sql("select i, xn, xd from test").show();

        session.close();
    }

    /**
     * 若查询记录中，各行数据结构不一致，根据sampleSize采样信息确定schema结构
     * 若某行的字段不在采样的schema中，则缺失字段补充为空值，多余字段将被忽略
     * Specifies a custom aggregation pipeline to apply to the collection before sending data to Spark.
     * MongoDB Connector支持下推，但为保障生产环境稳定，个人依然建议在pipeline中加入必要的限定条件
     */
    @Test
    public void combineTest() throws AnalysisException {
        Properties properties = PropertiesUtil.load(getClass().getClassLoader(), "ddl.properties");
        SparkSession session = SparkSession.builder().appName("sql-test").master("local").getOrCreate();
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://root:admin888@127.0.0.1:27017";
        String database = "fs_project";
        String collection = "fs_test";
        config.put(INPUT3_PREFIX + "uri", uri);
        config.put(INPUT3_PREFIX + "database", database);
        config.put(INPUT3_PREFIX + "collection", collection);

        String pipeline = "{ $match: { i : { $lt : 0 } } }";
        JavaMongoRDD<Document> rdd = MongoSpark.load(
                JavaSparkContext.fromSparkContext(session.sparkContext()), ReadConfig.create(config));
        rdd = rdd.withPipeline(Collections.singletonList(Document.parse(pipeline)));

        String ddl = properties.getProperty(database + "." + collection);

        Dataset<Row> dataset;
        if (!DPUtil.empty(ddl)) {
            StructType schema = StructType.fromDDL(ddl);
            ExpressionEncoder<Row> encoder = RowEncoder.apply(schema);
            dataset = session.createDataset(rdd.map((Function<Document, Row>) document -> {
                List<Object> data = new ArrayList<>();
                for (StructField field : schema.fields()) {
                    data.add(document.get(field.name()));
                }
                return SparkUtil.row(data);
            }).rdd(), encoder);
        } else {
            dataset = rdd.toDF();
        }

        dataset.createTempView("test");
        session.sql("select i, xn, xd from test").show();

        session.close();
    }

    @Test
    public void pipelineTest() throws AnalysisException {
        SparkSession session = SparkSession.builder().appName("pipeline-test").master("local").getOrCreate();
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://root:admin888@127.0.0.1:27017";
        String database = "fs_project";
        String collection = "fs_test";
        config.put(INPUT_PREFIX + "connection.uri", uri);
        config.put(INPUT_PREFIX + "database", database);
        config.put(INPUT_PREFIX + "collection", collection);
        config.put(INPUT_PREFIX + "sampleSize", "1");

        String pipeline = "{ $match: { i : { $gte : 1998999 } } }";
        config.put(INPUT_PREFIX + "aggregation.pipeline", pipeline);
        Dataset<Row> dataset = session.read().format("mongodb").options(config).load();
        dataset.foreachPartition((ForeachPartitionFunction<Row>) t -> {
            while (t.hasNext()) {
                Row row = t.next();
                if (!t.hasNext()) {
                    System.out.println("a" + row.get(1));
                }
            }
        });
        dataset.printSchema();
        dataset.show(10000);

        session.close();
    }

    @Test
    public void providerTest() throws AnalysisException {
        SparkSession session = SparkSession.builder().appName("pipeline-test").master("local").getOrCreate();
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://root:admin888@127.0.0.1:27017";
        String database = "fs_project";
        String collection = "fs_test";
        config.put(INPUT_PREFIX + "connection.uri", uri);
        config.put(INPUT_PREFIX + "database", database);
        config.put(INPUT_PREFIX + "collection", collection);
        String pipeline = "{ $match: { i : { $gte : 1998999 } } }";
        config.put(INPUT_PREFIX + "aggregation.pipeline", pipeline);

        Properties properties = PropertiesUtil.load(getClass().getClassLoader(), "ddl.properties");
        FSMongoTableProvider.registerDDL(uri, properties);

        Dataset<Row> dataset = session.read().format(FSMongoTableProvider.class.getName()).options(config).load();
        dataset.printSchema();
        dataset.show();

        session.close();
        FSMongoTableProvider.release();
    }

    @Test
    public void writeTest() {
        SparkSession session = SparkSession.builder().appName("write-test").master("local").getOrCreate();
        Map<String, String> config = new LinkedHashMap<>();
        String uri = "mongodb://root:admin888@127.0.0.1:27017";
        String database = "fs_project";
        String collection = "fs_t";
        config.put(OUTPUT_PREFIX + "connection.uri", uri);
        config.put(OUTPUT_PREFIX + "database", database);
        config.put(OUTPUT_PREFIX + "collection", collection);

        long time = System.currentTimeMillis();
        Dataset<Row> dataset = session.createDataset(Arrays.asList(
                RowFactory.create(1, "a", time, time),
                RowFactory.create(2, "b", time, time),
                RowFactory.create(3, "c", time, time)
        ), RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("_id", DataTypes.IntegerType, false),
                DataTypes.createStructField("name", DataTypes.StringType, false),
                DataTypes.createStructField("createdTime", DataTypes.LongType, false),
                DataTypes.createStructField("updatedTime", DataTypes.LongType, false)
        ))));
        dataset.show();
        dataset.write().format("mongodb").mode("append").options(config).save();
        session.close();
    }

}
