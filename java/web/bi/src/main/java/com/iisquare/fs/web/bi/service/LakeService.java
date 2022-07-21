package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import io.delta.tables.DeltaTable;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalog.Table;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * 数据湖近实时流处理方案，采用Delta数据湖同步Spark全局表进行内存映射
 * 同步方式：
 *      1. 采用批处理方式将DB数据写入到数据湖
 *      2. 采用批处理方式清理非字典表的历史数据，维护数据湖表结构
 *      3. 采用流处理方式将Kafka中的CDC日志同步到数据湖中
 * 映射方式：
 *      1. 采用流处理方式加载数据湖并注册为全局表
 * 计算方式：
 *      1. 采用批处理方式，在同一个Session中直接查询全局表
 * 测试结果：
 *      1. 数据湖基础延迟较大，且需要处理多任务并发读写问题
 *      2. 小数据量更新耗时5s左右，不适合小数据量的复杂查询需求
 *      3. 可将该方案的数据湖替换为事件驱动，按窗口定时拉取数据变更日志
 *      4. 通过原生API直接读取数据库，将原始数据注册为全局表，隔断数据库连接
 *      5. 全局表即内存数据，适合频繁对近期内变更数据进行复杂查询的场景
 */
@Service
public class LakeService implements Serializable {

    @Autowired
    private SparkService sparkService;
    public static final String DELTA_PATH = "/fs_project/";
    public final List<String> tables = Arrays.asList(
            "fs_member_user", // 模拟字典项
            "fs_bi_diagram" // 模拟业务数据变更
    );
    public static final transient Map<String, DeltaTable> deltaTables = new LinkedHashMap<>();

    /**
     * 批处理同步数据库到数据湖
     */
    public Map<String, Object> db2lake(Map<?, ?> arg) {
        SparkSession session = sparkService.session().newSession();
        for (String table : tables) {
            Dataset<Row> dataset = session.read().format("jdbc").options(new LinkedHashMap<String, String>() {{
                put("url", "jdbc:mysql://127.0.0.1:3306/fs_project?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true");
                put("query", "select * from " + table);
                put("driver", "com.mysql.jdbc.Driver");
                put("user", "root");
                put("password", "admin888");
            }}).load();
            DeltaTable deltaTable = DeltaTable.forPath(session, DELTA_PATH + table);
            deltaTable.delete("id = ''");
            Map<String, String> schema2delta = schema2delta(dataset.schema(), "b");
            deltaTable.as("a").merge(dataset.as("b"), "a.id = b.id")
                    .whenMatched().updateExpr(schema2delta)
                    .whenNotMatched().insertExpr(schema2delta).execute();
        }
        return ApiUtil.result(0, null, tables);
    }

    /**
     * 为避免并发冲突，Delta数据湖每个表只能由一个服务进行批处理操作
     * DeltaTable cannot be used in executors
     * 通过接口方式将流处理转换为批处理，分布式环境下，需处理数据湖表的分配情况
     */
    public Map<String, Object> sync2lake(JsonNode json) {
        String table = json.at("/source/table").asText("");
        if (!tables.contains(table)) return ApiUtil.result(1001, "table missing", table);
        DeltaTable deltaTable = deltaTables.get(table);
        if (null == deltaTable) {
            synchronized (LakeService.class) {
                if (null == deltaTable) {
                    deltaTable = DeltaTable.forPath(sparkService.session(), DELTA_PATH + table);
                    deltaTables.put(table, deltaTable);
                }
            }
        }
        if (json.at("/after").isNull()) {
            deltaTable.delete(String.format("id = '%d'", json.at("/before/id").asInt()));
            return ApiUtil.result(0, "delete", json.at("/before/id").asInt());
        }
        if (json.at("/before").isNull()) {
            deltaTable.updateExpr(json2string(json.at("/after")));
            return ApiUtil.result(0, "insert", json.at("/after/id").asInt());
        }
        deltaTable.updateExpr(String.format("id = '%d'", json.at("/before/id").asInt()), json2string(json.at("/after")));
        return ApiUtil.result(0, "update", json.at("/after/id").asInt());
    }

    /**
     * 流处理同步CDC到数据湖
     */
    public Map<String, Object> kafka2lake(Map<?, ?> arg) throws TimeoutException {
        SparkSession session = sparkService.session().newSession();
        Dataset<Row> dataset = session.readStream().format("kafka").options(new LinkedHashMap<String, String>() {{
            put("kafka.bootstrap.servers", "127.0.0.1:9092");
            put("subscribe", "fs_binlog");
            put("startingOffsets", "earliest");
        }}).load();
        dataset.writeStream().foreach(new ForeachWriter<Row>() {

            CloseableHttpClient client = null;

            @Override
            public boolean open(long partitionId, long epochId) {
                if (null == client) client = HttpClients.createDefault();
                return true;
            }

            @Override
            public void process(Row value) {
                String message = new String((byte[]) value.get(1));
                HttpPost http = new HttpPost("http://127.0.0.1:7815/test/sync2lake");
                http.setHeader(HttpHeaders.CONTENT_TYPE, "application/json;");
                http.setEntity(new StringEntity(message, "UTF-8"));
                try {
                    CloseableHttpResponse response = client.execute(http);
                    System.out.println(String.format(
                            "sync status %d %s",
                            response.getStatusLine().getStatusCode(),
                            EntityUtils.toString(response.getEntity(), "UTF-8")));
                    FileUtil.close(response);
                } catch (IOException e) {
                    throw new RuntimeException("sync to delta failed", e);
                }
            }

            @Override
            public void close(Throwable errorOrNull) {
                FileUtil.close(client);
                client = null;
            }
        }).option("checkpointLocation", DELTA_PATH + "checkpoints").start();
        return ApiUtil.result(0, null, null);
    }

    public static Map<String, String> json2string(JsonNode json) {
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        Map<String, String> result = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            Object value = DPUtil.toJSON(entry.getValue(), Object.class);
            if (null == value) {
                result.put(key, null);
            } else {
                result.put(key, String.format("'%s'", DPUtil.parseString(value)));
            }
        }
        return result;
    }

    public static Map<String, String> schema2delta(StructType schema, String table) {
        Map<String, String> result = new LinkedHashMap<>();
        for (StructField field : schema.fields()) {
            String name = field.name();
            result.put(name, String.format("%s.%s", table, name));
        }
        return result;
    }

    /**
     * 流处理读取数据湖注册为全局表
     */
    public Map<String, Object> lake2table(Map<?, ?> arg) throws AnalysisException {
        SparkSession session = sparkService.session().newSession();

        Dataset<Row> user = session.readStream().format("delta").load(DELTA_PATH + "fs_member_user"); // 字典
        user.createOrReplaceGlobalTempView("fs_member_user");

        Dataset<Row> diagram = session.readStream().format("delta").load(DELTA_PATH + "fs_bi_diagram");
        diagram.selectExpr("*", "to_timestamp(updated_time) as timestamp")
                .withWatermark("timestamp", "5 seconds")
                .groupBy(functions.window(new Column("timestamp"), "5 seconds", "1 minutes"))
                .df().createOrReplaceGlobalTempView("fs_bi_diagram");

        List<String> result = new ArrayList<>();
        for (Table table : session.catalog().listTables("global_temp").collectAsList()) {
            result.add(table.name());
        }
        return ApiUtil.result(0, null, result);
    }

    /**
     * 批处理采用全局表进行SQL查询
     */
    public Map<String, Object> table2query(Map<?, ?> arg) throws TimeoutException {
        SparkSession session = sparkService.session().newSession();
        String sql = String.format("select * from" +
                " global_temp.fs_bi_diagram as diagram join global_temp.fs_member_user as user" +
                " on diagram.created_uid = user.id and diagram.timestamp > to_timestamp(%d)", System.currentTimeMillis() - 30000);
//        String sql = "select * from global_temp.fs_member_user";
        Dataset<Row> dataset = session.sql(sql);
        dataset.writeStream().foreach(new ForeachWriter<Row>() {
            @Override
            public boolean open(long partitionId, long epochId) {
                return true;
            }

            @Override
            public void process(Row value) {
                System.out.println("sql result:" + value);
            }

            @Override
            public void close(Throwable errorOrNull) {

            }
        }).start();
        return ApiUtil.result(0, null, null);
    }

}
