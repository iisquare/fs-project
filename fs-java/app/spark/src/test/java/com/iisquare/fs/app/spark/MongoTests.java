package com.iisquare.fs.app.spark;

import com.iisquare.fs.app.spark.mongo.FSMongoTableProvider;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.PropertiesUtil;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCursor;
import com.mongodb.spark.sql.connector.config.MongoConfig;
import com.mongodb.spark.sql.connector.config.ReadConfig;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

public class MongoTests implements Serializable {

    /**
     * 连接与库表配置。
     *
     * MongoDB Spark Connector 10.x 使用 DataSource V2 接口，
     * 配置键形如 spark.mongodb.read.connection.uri / database / collection，
     * 旧的 spark.mongodb.input.* 前缀与 MongoSpark/ReadConfig 已废弃。
     */
    public static final String URI = "mongodb://root:admin888@127.0.0.1:27017";
    public static final String DATABASE = "fs_project";
    public static final String PIPELINE_LIMIT = "{ $limit: 1000 }";
    public static final String PIPELINE_MATCH = "{ $match: { i : { $gte : 1998999 } } }";

    public SparkSession session(String appName) {
        return SparkSession.builder().appName(appName).master("local").getOrCreate();
    }

    public Map<String, String> readConfig(String database, String collection) {
        Map<String, String> config = new LinkedHashMap<>();
        config.put(MongoConfig.READ_PREFIX + MongoConfig.CONNECTION_STRING_CONFIG, URI);
        config.put(MongoConfig.READ_PREFIX + MongoConfig.DATABASE_NAME_CONFIG, database);
        config.put(MongoConfig.READ_PREFIX + MongoConfig.COLLECTION_NAME_CONFIG, collection);
        return config;
    }

    public Map<String, String> writeConfig(String database, String collection) {
        Map<String, String> config = new LinkedHashMap<>();
        config.put(MongoConfig.WRITE_PREFIX + MongoConfig.CONNECTION_STRING_CONFIG, URI);
        config.put(MongoConfig.WRITE_PREFIX + MongoConfig.DATABASE_NAME_CONFIG, database);
        config.put(MongoConfig.WRITE_PREFIX + MongoConfig.COLLECTION_NAME_CONFIG, collection);
        return config;
    }

    /**
     * 读取集合的schema并输出为DDL：采样结果用于静态化集合结构
     */
    public String ddl(SparkSession session, String database, String collection) {
        Map<String, String> config = readConfig(database, collection);
        config.put(MongoConfig.READ_PREFIX + ReadConfig.AGGREGATION_PIPELINE_CONFIG, PIPELINE_LIMIT);
        return session.read().format("mongodb").options(config).load().schema().toDDL();
    }

    @Test
    public void encodeTest() {
        MongoClient client = MongoClients.create(URI + "/");
        MongoCursor<String> iterator = client.getDatabase(DATABASE).listCollectionNames().iterator();
        SparkSession session = session("ddl-test");
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNext()) {
            String collection = iterator.next();
            System.out.println(String.format("ddl %s.%s", DATABASE, collection));
            // 当集合内无数据时，依然无法获取到结构信息，可适当补充测试数据用于静态化
            sb.append(String.format("\n%s.%s=%s", DATABASE, collection, ddl(session, DATABASE, collection)));
        }
        System.out.println(sb.toString());
        session.close();
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
        SparkSession session = session("sql-test");
        String database = DATABASE;
        String collection = "fs_test";
        Map<String, String> config = readConfig(database, collection);
        config.put(MongoConfig.READ_PREFIX + ReadConfig.AGGREGATION_PIPELINE_CONFIG, "{ $match: { i : { $lt : 0 } } }");
        Dataset<Row> dataset = session.read().format("mongodb").options(config).load();

        String ddl = properties.getProperty(database + "." + collection);

        if (!DPUtil.empty(ddl)) {
            dataset.printSchema();
            dataset = cast(dataset, StructType.fromDDL(ddl));
        }

        dataset.createTempView("test");
        session.sql("select i, xn, xd from test").show();

        session.close();
    }

    /**
     * 采样结构与固定结构：
     * 1. 连接器按 sampleSize 采样推断schema；行内字段不在采样结果中时补空值，多余字段被忽略；
     * 2. aggregation.pipeline 会下推到MongoDB，生产环境建议带上必要的限定条件；
     * 3. 担心采样缺列时，可用开发环境导出的DDL重投影为固定结构，保证后续SQL列结构稳定。
     */
    @Test
    public void combineTest() throws AnalysisException {
        Properties properties = PropertiesUtil.load(getClass().getClassLoader(), "ddl.properties");
        SparkSession session = session("combine-test");
        String database = DATABASE;
        String collection = "fs_test";
        Map<String, String> config = readConfig(database, collection);
        config.put(MongoConfig.READ_PREFIX + ReadConfig.AGGREGATION_PIPELINE_CONFIG, "{ $match: { i : { $lt : 0 } } }");
        config.put(MongoConfig.READ_PREFIX + ReadConfig.INFER_SCHEMA_SAMPLE_SIZE_CONFIG, "1");
        Dataset<Row> dataset = session.read().format("mongodb").options(config).load();
        dataset.printSchema(); // 采样得到的结构

        String ddl = properties.getProperty(database + "." + collection);
        if (!DPUtil.empty(ddl)) {
            dataset = cast(dataset, StructType.fromDDL(ddl));
        }

        dataset.createTempView("test");
        session.sql("select i, xn, xd from test").show();

        session.close();
    }

    /**
     * 按固定DDL重投影：缺失字段补空值、多余字段忽略，避免采样缺列导致后续SQL找不到列
     */
    public Dataset<Row> cast(Dataset<Row> dataset, StructType schema) {
        List<String> names = Arrays.asList(dataset.columns());
        List<Column> columns = new ArrayList<>();
        for (StructField field : schema.fields()) {
            if (names.contains(field.name())) {
                columns.add(functions.col(field.name()).cast(field.dataType()).as(field.name()));
            } else {
                columns.add(functions.lit(null).cast(field.dataType()).as(field.name()));
            }
        }
        return dataset.select(columns.toArray(new Column[0]));
    }

    @Test
    public void pipelineTest() throws AnalysisException {
        SparkSession session = session("pipeline-test");
        String database = DATABASE;
        String collection = "fs_test";
        Map<String, String> config = readConfig(database, collection);
        config.put(MongoConfig.READ_PREFIX + ReadConfig.INFER_SCHEMA_SAMPLE_SIZE_CONFIG, "1");
        config.put(MongoConfig.READ_PREFIX + ReadConfig.AGGREGATION_PIPELINE_CONFIG, PIPELINE_MATCH);
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
        SparkSession session = session("provider-test");
        String database = DATABASE;
        String collection = "fs_test";
        Map<String, String> config = readConfig(database, collection);
        config.put(MongoConfig.READ_PREFIX + ReadConfig.AGGREGATION_PIPELINE_CONFIG, PIPELINE_MATCH);

        Properties properties = PropertiesUtil.load(getClass().getClassLoader(), "ddl.properties");
        FSMongoTableProvider.registerDDL(URI, properties);

        Dataset<Row> dataset = session.read().format(FSMongoTableProvider.class.getName()).options(config).load();
        dataset.printSchema();
        dataset.show();

        session.close();
        FSMongoTableProvider.release();
    }

    @Test
    public void writeTest() {
        SparkSession session = session("write-test");
        Map<String, String> config = writeConfig(DATABASE, "fs_t");

        long time = System.currentTimeMillis();
        Dataset<Row> dataset = session.createDataset(Arrays.asList(
                RowFactory.create(1, "a", time, time),
                RowFactory.create(2, "b", time, time),
                RowFactory.create(3, "c", time, time)
        ), RowEncoder.encoderFor(DataTypes.createStructType(Arrays.asList(
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
