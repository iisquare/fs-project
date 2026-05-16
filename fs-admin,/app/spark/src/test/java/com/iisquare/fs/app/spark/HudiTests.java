package com.iisquare.fs.app.spark;

import com.iisquare.fs.app.spark.demo.DemoConfig;
import com.iisquare.fs.app.spark.demo.DemoSource;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.hudi.DataSourceWriteOptions;
import org.apache.hudi.common.table.HoodieTableConfig;
import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.junit.After;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HudiTests implements Closeable {

    SparkSession session;
    final String basePath = "file:///D:/htdocs/static/hudi";

    public HudiTests() {
        SparkConf config = DemoConfig.spark().setAppName(HudiTests.class.getSimpleName());
        config.set("spark.serializer", KryoSerializer.class.getName());
        session = SparkSession.builder().config(config).getOrCreate();
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
            Dataset<Row> dataset = DemoSource.mysql(session, sql, 500);
            upsert(dataset, entry.getKey(), "name");
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
            Dataset<Row> dataset = DemoSource.mysql(session, sql, 500);
            dataset.createOrReplaceTempView(entry.getKey());
        }
        String sql = "select acc.name, acc.score, acc.word_count, gram.title, '' as ts" +
                " from t_word_acc as acc join t_word_gram as gram on acc.name = gram.name where acc.score > 100";
        Dataset<Row> dataset = session.sql(sql);
        overwrite(dataset, "fs_word", "name");
    }

    /**
     * 将相同主键的多个表的不同字段写入同一张宽表（不支持）
     * SchemaCompatibilityException: Incoming batch schema is not compatible with the table's one
     */
    @Test
    public void mergeTest() {
        LinkedHashMap<String, List<String>> tables = new LinkedHashMap<String, List<String>>() {{
            put("t_word_acc", Arrays.asList("name", "score", "word_count"));
            put("t_word_gram", Arrays.asList("name", "title"));
        }};
        for (Map.Entry<String, List<String>> entry : tables.entrySet()) {
            String sql = String.format("select %s, '' as ts from %s", DPUtil.implode(", ", entry.getValue()), entry.getKey());
            Dataset<Row> dataset = DemoSource.mysql(session, sql, 500);
            upsert(dataset, "fs_word", "name");
        }
    }

    @Test
    public void readTest() {
        for (String name : Arrays.asList("t_word_acc", "t_word_gram", "fs_word")) {
            Dataset<Row> dataset = session.read().format("hudi").load(basePath + "/" + name);
            if ("fs_word".equals(name)) {
                dataset.where("name != ''").show();
            } else {
                dataset.show();
            }
        }
    }

    public void upsert(Dataset<Row> dataset, String table, String pk) {
        dataset.write().format("hudi").mode(SaveMode.Append)
                .option(DataSourceWriteOptions.OPERATION().key(), "upsert")
                .option(HoodieTableConfig.HOODIE_TABLE_NAME_KEY, table)
                .option(DataSourceWriteOptions.RECORDKEY_FIELD().key(), pk)
                .option(DataSourceWriteOptions.PARTITIONPATH_FIELD().key(), "ts")
                .save(basePath + "/" + table);
    }

    public void overwrite(Dataset<Row> dataset, String table, String pk) {
        dataset.write().format("hudi").mode(SaveMode.Overwrite)
                .option(DataSourceWriteOptions.OPERATION().key(), "upsert")
                .option(HoodieTableConfig.HOODIE_TABLE_NAME_KEY, table)
                .option(DataSourceWriteOptions.RECORDKEY_FIELD().key(), pk)
                .option(DataSourceWriteOptions.PARTITIONPATH_FIELD().key(), "ts")
                .save(basePath + "/" + table);
    }

}
