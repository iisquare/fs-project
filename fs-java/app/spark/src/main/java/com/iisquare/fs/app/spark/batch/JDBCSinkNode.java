package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGException;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractJDBCSink;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 通用 JDBC 写入节点。
 *
 * mode=append/overwrite 使用 Spark JDBC 写入；mode=upsert 根据 keyFields
 * 使用数据库原生 upsert 语法，支持 MySQL 与 PostgreSQL。
 */
public class JDBCSinkNode extends AbstractJDBCSink<Void> {

    private String url;
    private String username;
    private String password;
    private String driver;
    private String table;
    private String mode;
    private List<String> keyFields = new ArrayList<>();
    private Set<String> excludeUpdates = new HashSet<>();
    private int batchSize;

    @Override
    public DAGType<Void> outputType() {
        return SparkTypes.VOID;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        url = options.at("/url").asText();
        username = options.at("/username").asText();
        password = options.at("/password").asText();
        driver = DAGCore.jdbcDriver(options.at("/driver").asText());
        table = options.at("/table").asText();
        mode = options.at("/mode").asText("append").toLowerCase(Locale.ROOT);
        keyFields = fields(options.at("/keyFields").asText(""));
        excludeUpdates = new LinkedHashSet<>(fields(options.at("/updateExclude").asText("")));
        batchSize = Math.max(1, options.at("/batchSize").asInt(500));
        if (DPUtil.empty(url) || DPUtil.empty(table)) return false;
        if (!table.matches("[A-Za-z0-9_.]+")) return false;
        return Arrays.asList("append", "overwrite", "upsert").contains(mode);
    }

    @Override
    public Void process() throws Exception {
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        if (!"upsert".equals(mode)) {
            Map<String, String> config = new LinkedHashMap<>();
            config.put("url", url);
            config.put("user", username);
            config.put("password", password);
            if (!DPUtil.empty(driver)) config.put("driver", driver);
            config.put("batchsize", String.valueOf(batchSize));
            dataset.write().format("jdbc").mode(mode).options(config)
                    .option("dbtable", table).save();
            return null;
        }
        upsert(dataset);
        return null;
    }

    private void upsert(Dataset<Row> dataset) throws Exception {
        List<String> columns = Arrays.asList(dataset.columns());
        if (columns.isEmpty()) return;
        for (String column : columns) {
            if (!column.matches("[A-Za-z0-9_]+")) {
                throw new DAGException(this, "非法字段名：" + column);
            }
        }
        List<String> keys = keyFields.isEmpty() ? List.of("id") : keyFields;
        for (String key : keys) {
            if (!columns.contains(key)) throw new DAGException(this, "upsert 主键字段不存在：" + key);
        }
        boolean postgres = url.startsWith("jdbc:postgresql:");
        String quote = postgres ? "\"" : "`";
        String tableName = quotedTable(table, quote);
        String columnList = columns.stream().map(item -> quote + item + quote)
                .reduce((left, right) -> left + "," + right).orElse("");
        String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        List<String> updates = columns.stream()
                .filter(item -> !keys.contains(item) && !excludeUpdates.contains(item))
                .toList();
        String sql;
        if (postgres) {
            String keysText = keys.stream().map(item -> quote + item + quote)
                    .reduce((left, right) -> left + "," + right).orElse("");
            String updateText = updates.stream()
                    .map(item -> quote + item + quote + "=EXCLUDED." + quote + item + quote)
                    .reduce((left, right) -> left + "," + right).orElse("");
            sql = "INSERT INTO " + tableName + " (" + columnList + ") VALUES (" + placeholders + ")"
                    + " ON CONFLICT (" + keysText + ") "
                    + (DPUtil.empty(updateText) ? "DO NOTHING" : "DO UPDATE SET " + updateText);
        } else {
            String updateText = updates.stream()
                    .map(item -> quote + item + quote + "=VALUES(" + quote + item + quote + ")")
                    .reduce((left, right) -> left + "," + right).orElse("");
            if (DPUtil.empty(updateText)) {
                updateText = quote + keys.get(0) + quote + "=" + quote + keys.get(0) + quote;
            }
            sql = "INSERT INTO " + tableName + " (" + columnList + ") VALUES (" + placeholders + ")"
                    + " ON DUPLICATE KEY UPDATE " + updateText;
        }
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                int count = 0;
                Iterator<Row> iterator = dataset.toLocalIterator();
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    for (int i = 0; i < columns.size(); i++) {
                        statement.setObject(i + 1, row.isNullAt(i) ? null : row.get(i));
                    }
                    statement.addBatch();
                    if (++count % batchSize == 0) statement.executeBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private String quotedTable(String value, String quote) {
        return Arrays.stream(value.split("\\."))
                .map(item -> quote + item + quote)
                .reduce((left, right) -> left + "." + right).orElse(value);
    }

    private List<String> fields(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

}
