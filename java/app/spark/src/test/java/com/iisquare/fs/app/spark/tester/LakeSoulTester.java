package com.iisquare.fs.app.spark.tester;

import com.dmetasoul.lakesoul.sql.LakeSoulSparkSessionExtension;
import com.dmetasoul.lakesoul.tables.LakeSoulTable;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SourceUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.hadoop.fs.s3a.AnonymousAWSCredentialsProvider;
import org.apache.hadoop.fs.s3a.S3AFileSystem;
import org.apache.hudi.DataSourceWriteOptions;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.lakesoul.catalog.LakeSoulCatalog;
import org.junit.After;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在使用 LakeSoul 之前需要初始化元数据表结构：
 * https://github.com/lakesoul-io/LakeSoul/blob/v2.4.1/script/meta_init.sql
 */
public class LakeSoulTester implements Closeable {

    SparkSession session;
    final String basePath = "s3a://data/lake_soul";

    public LakeSoulTester() {
        SparkConf config = ConfigUtil.spark().setAppName(LakeSoulTester.class.getSimpleName());
        config.set("spark.sql.extensions", LakeSoulSparkSessionExtension.class.getName());
        config.set("spark.sql.catalog.lakesoul", LakeSoulCatalog.class.getName());
        config.set("spark.sql.defaultCatalog", "lakesoul");
        config.set("spark.hadoop.fs.s3.impl", S3AFileSystem.class.getName());
        config.set("spark.hadoop.fs.s3a.path.style.access", "true");
        config.set("spark.hadoop.fs.s3a.endpoint", "http://127.0.0.1:9000");
        config.set("spark.hadoop.fs.s3a.aws.credentials.provider", AnonymousAWSCredentialsProvider.class.getName());
        config.set("spark.hadoop.fs.s3a.access.key", "L81NP9NTA1KQPXNWJSGJ");
        config.set("spark.hadoop.fs.s3a.secret.key", "ca0+qM2u3rsMTZ+nIrihDFk1WySOXhkhgtn8dy5p");
        session = SparkSession.builder().config(config).getOrCreate();
        // DBUtil.getDBInfo();
        System.setProperty("lakesoul.pg.driver", "com.lakesoul.shaded.org.postgresql.Driver");
        System.setProperty("lakesoul.pg.url", "jdbc:postgresql://127.0.0.1:5432/lakesoul?stringtype=unspecified");
        System.setProperty("lakesoul.pg.username", "postgres");
        System.setProperty("lakesoul.pg.password", "admin888");
    }

    @After
    @Override
    public void close() throws IOException {
        session.close();
    }

    @Test
    public void batchTest() {
        LinkedHashMap<String, List<String>> tables = new LinkedHashMap<String, List<String>>() {{
            put("t_word_acc", Arrays.asList("name", "score", "word_count"));
            put("t_word_gram", Arrays.asList("name", "title"));
        }};
        for (Map.Entry<String, List<String>> entry : tables.entrySet()) {
            String sql = String.format("select %s, '' as ts from %s", DPUtil.implode(", ", entry.getValue()), entry.getKey());
            Dataset<Row> dataset = SourceUtil.mysql(session, sql, 500);
            overwrite(dataset, entry.getKey(), "name");
        }
    }

    /**
     * 通过计算引擎生成大宽表
     */
    @Test
    public void joinTest() {
        LinkedHashMap<String, List<String>> tables = new LinkedHashMap<String, List<String>>() {{
            put("t_word_acc", Arrays.asList("name", "score", "word_count"));
            put("t_word_gram", Arrays.asList("name", "title"));
        }};
        for (Map.Entry<String, List<String>> entry : tables.entrySet()) {
            String sql = String.format("select %s, '' as ts from %s", DPUtil.implode(", ", entry.getValue()), entry.getKey());
            Dataset<Row> dataset = SourceUtil.mysql(session, sql, 500);
            dataset.createOrReplaceTempView(entry.getKey());
        }
        String sql = "select acc.name, acc.score, acc.word_count, gram.title, '' as ts" +
                " from t_word_acc as acc join t_word_gram as gram on acc.name = gram.name where acc.score > 100";
        Dataset<Row> dataset = session.sql(sql);
        overwrite(dataset, "fs_word", "name");
    }

    /**
     * 将相同主键的多个表的不同字段写入同一张宽表
     */
    @Test
    public void mergeTest() {
        LakeSoulTable table = LakeSoulTable.forPath(basePath + "/fs_word");
        LinkedHashMap<String, List<String>> tables = new LinkedHashMap<String, List<String>>() {{
            put("t_word_acc", Arrays.asList("name", "score", "word_count"));
            put("t_word_gram", Arrays.asList("name", "title"));
        }};
        for (Map.Entry<String, List<String>> entry : tables.entrySet()) {
            String sql = String.format("select %s, '' as ts from %s", DPUtil.implode(", ", entry.getValue()), entry.getKey());
            Dataset<Row> dataset = SourceUtil.mysql(session, sql, 500);
            table.upsert(dataset, "");
        }
    }

    @Test
    public void readTest() {
        for (String name : Arrays.asList("t_word_acc", "t_word_gram", "fs_word")) {
            LakeSoulTable table = LakeSoulTable.forPath(basePath + "/" + name);
            Dataset<Row> dataset = table.toDF();
            if ("fs_word".equals(name)) {
                dataset.where("name != ''").show();
            } else {
                dataset.show();
            }
        }
    }

    public void upsert(Dataset<Row> dataset, String table, String pk) {
        dataset.write().format("lakesoul").mode(SaveMode.Append)
                .option(DataSourceWriteOptions.RECORDKEY_FIELD().key(), pk)
                .option("hashPartitions", "name")
                .option("hashBucketNum","1")
                .option("mergeSchema", "true")
                .save(basePath + "/" + table);
    }

    public void overwrite(Dataset<Row> dataset, String table, String pk) {
        dataset.write().format("lakesoul").mode(SaveMode.Overwrite)
                .option(DataSourceWriteOptions.RECORDKEY_FIELD().key(), pk)
                .option("hashPartitions", "name")
                .option("hashBucketNum","1")
                .option("overwriteSchema", "true")
                .save(basePath + "/" + table);
    }

}
