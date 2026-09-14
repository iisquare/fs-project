package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.sse.MaintainEmitter;
import com.iisquare.fs.web.bi.core.RedisKey;
import com.iisquare.fs.web.bi.dao.DatasourceDao;
import com.iisquare.fs.web.bi.datasource.DatasourceConnector;
import com.iisquare.fs.web.bi.datasource.ElasticsearchConnector;
import com.iisquare.fs.web.bi.datasource.JDBCConnector;
import com.iisquare.fs.web.bi.datasource.MongoDBConnector;
import com.iisquare.fs.web.bi.entity.Datasource;
import com.iisquare.fs.web.bi.entity.Dataset;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TrinoService extends ServiceBase {

    public static final String ICEBERG_CATALOG = "iceberg";
    public static final String DATASET_SCHEMA = "dataset";

    @Autowired
    @Qualifier("trinoDataSource")
    DataSource trinoDataSource;
    @Autowired
    DatasourceDao datasourceDao;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    TrinoIntegrationService trinoIntegrationService;
    @Value("${fs.bi.trino.iceberg.hive-metastore-uri:}")
    String icebergHiveMetastoreUri;
    @Value("${fs.bi.trino.iceberg.s3-endpoint:}")
    String icebergS3Endpoint;
    @Value("${fs.bi.trino.iceberg.s3-region:us-east-1}")
    String icebergS3Region;
    @Value("${fs.bi.trino.iceberg.s3-access-key:}")
    String icebergS3AccessKey;
    @Value("${fs.bi.trino.iceberg.s3-secret-key:}")
    String icebergS3SecretKey;
    @Value("${fs.bi.trino.iceberg.s3-path-style-access:true}")
    String icebergS3PathStyleAccess;
    @Value("${fs.bi.trino.iceberg.bucket:iceberg}")
    String icebergBucket;

    public MaintainEmitter reload(Map<?, ?> param, MaintainEmitter emitter) {
        Boolean locked = redis.opsForValue().setIfAbsent(
                RedisKey.trinoCatalogReloadLock(),
                String.valueOf(System.currentTimeMillis()),
                100,
                TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            emitter.error(1502, "目录正在重新加载中", null);
            return emitter;
        }
        try {
            Map<String, String> steps = new LinkedHashMap<>();
            steps.put("dropAllCatalogs", "清理历史目录");
            steps.put("createFsBi", "创建内置目录 fs_bi");
            steps.put("createIceberg", "创建 Iceberg 目录");
            steps.put("datasource", "注册数据源目录");
            emitter.plan(steps);
            emitter.start("开始重新加载Trino目录", "reload");

            dropAllCatalogs(emitter);

            Map<String, String> failures = new LinkedHashMap<>();

            Map<String, String> fsBiProperties = new LinkedHashMap<>();
            fsBiProperties.put("base-uri", trinoIntegrationService.getIntegrationSelf());
            fsBiProperties.put("api-key", trinoIntegrationService.getIntegrationKey());
            CatalogDefinition fsBiCatalog = new CatalogDefinition("fs_bi", "fs_http", fsBiProperties);
            emitter.step("正在创建内置目录 fs_bi", "createFsBi", 10, 1);
            try {
                createCatalog(fsBiCatalog);
                emitter.log("内置目录 fs_bi 创建成功", "createFsBi", 15, "success");
            } catch (Exception e) {
                failures.put("fs_bi", e.getMessage());
                emitter.log("内置目录 fs_bi 创建失败：" + e.getMessage(), "createFsBi", 15, "warning");
            }

            if (!DPUtil.empty(icebergHiveMetastoreUri)) {
                Map<String, String> icebergProperties = new LinkedHashMap<>();
                icebergProperties.put("iceberg.catalog.type", "hive_metastore");
                icebergProperties.put("hive.metastore.uri", icebergHiveMetastoreUri);
                icebergProperties.put("fs.s3.enabled", "true");
                if (!DPUtil.empty(icebergS3Endpoint)) {
                    icebergProperties.put("s3.endpoint", icebergS3Endpoint);
                }
                if (!DPUtil.empty(icebergS3Region)) {
                    icebergProperties.put("s3.region", icebergS3Region);
                }
                if (!DPUtil.empty(icebergS3AccessKey)) {
                    icebergProperties.put("s3.aws-access-key", icebergS3AccessKey);
                }
                if (!DPUtil.empty(icebergS3SecretKey)) {
                    icebergProperties.put("s3.aws-secret-key", icebergS3SecretKey);
                }
                if (!DPUtil.empty(icebergS3PathStyleAccess)) {
                    icebergProperties.put("s3.path-style-access", icebergS3PathStyleAccess);
                }
                CatalogDefinition icebergCatalog = new CatalogDefinition("iceberg", "iceberg", icebergProperties);
                emitter.step("正在创建 Iceberg 目录", "createIceberg", 20, 1);
                try {
                    createCatalog(icebergCatalog);
                    ensureDatasetSchema();
                    emitter.log("Iceberg 目录创建成功", "createIceberg", 30, "success");
                } catch (Exception e) {
                    failures.put("iceberg", e.getMessage());
                    emitter.log("Iceberg 目录创建失败：" + e.getMessage(), "createIceberg", 30, "warning");
                }
            }

            List<Datasource> datasourceList = datasourceDao.findAll((Specification<Datasource>) (root, query, cb) -> {
                return cb.and(cb.equal(root.get("status"), 1), cb.equal(root.get("olapable"), 1));
            });
            int total = datasourceList.size();
            int success = 0;
            emitter.step("正在注册数据源目录", "datasource", 30, total);
            for (int index = 0; index < total; index++) {
                Datasource datasource = datasourceList.get(index);
                int percent = 30 + (int) Math.round(60.0 * (index + 1) / Math.max(total, 1));
                emitter.log("正在注册数据源：" + datasource.getName() + " (" + (index + 1) + "/" + total + ")", "datasource", percent);
                try {
                    JsonNode config = DPUtil.parseJSON(datasource.getContent(), k -> DPUtil.objectNode());
                    DatasourceConnector<?> connector = DatasourceConnector.connector(datasource.getType(), config);
                    List<CatalogDefinition> definitions = catalogDefinitions(datasource, config, connector);
                    if (definitions.isEmpty()) {
                        continue;
                    }
                    int created = 0;
                    for (CatalogDefinition definition : definitions) {
                        createCatalog(definition);
                        created++;
                    }
                    success++;
                    emitter.log("数据源 " + datasource.getName() + " 注册成功，创建 " + created + " 个目录", "datasource", percent, "success", index + 1, total);
                } catch (Exception e) {
                    failures.put(datasource.getName(), e.getMessage());
                    emitter.log("数据源 " + datasource.getName() + " 注册失败：" + e.getMessage(), "datasource", percent, "warning", index + 1, total);
                }
            }
            emitter.log("数据源注册完成：成功 " + success + " 个，失败 " + failures.size() + " 个", "datasource", 95);

            ObjectNode data = DPUtil.objectNode();
            failures.forEach(data::put);
            if (!failures.isEmpty()) {
                emitter.result(1500, "部分数据源注册失败", data);
                return emitter;
            }
            emitter.result(0, "全部成功", data);
            return emitter;
        } catch (Exception e) {
            emitter.error(1500, "重新加载目录失败：" + e.getMessage(), e.getMessage());
            return emitter;
        } finally {
            redis.delete(RedisKey.trinoCatalogReloadLock());
        }
    }

    private List<CatalogDefinition> catalogDefinitions(Datasource datasource, JsonNode config, DatasourceConnector<?> connector) throws Exception {
        if (connector instanceof JDBCConnector jdbcConnector) {
            return jdbcCatalogDefinitions(datasource, config, jdbcConnector);
        }
        if (connector instanceof MongoDBConnector) {
            return mongoCatalogDefinitions(datasource, config, (MongoDBConnector) connector);
        }
        if (connector instanceof ElasticsearchConnector) {
            return elasticsearchCatalogDefinitions(datasource, config);
        }
        return List.of();
    }

    private List<CatalogDefinition> jdbcCatalogDefinitions(Datasource datasource, JsonNode config, JDBCConnector connector) throws Exception {
        String connectorName = trinoConnectorName(datasource.getType());
        if ("mysql".equals(datasource.getType()) || "doris".equals(datasource.getType())) {
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put("connection-url", connector.baseUrl());
            properties.put("connection-user", config.at("/username").asText());
            properties.put("connection-password", config.at("/password").asText());
            return List.of(new CatalogDefinition(datasource.getName(), connectorName, properties));
        }
        String database = config.at("/database").asText("");
        List<String> databases = DPUtil.empty(database) ? connector.databases() : List.of(database);
        List<CatalogDefinition> definitions = new ArrayList<>();
        for (String db : databases) {
            if (DPUtil.empty(db)) {
                continue;
            }
            Map<String, String> properties = new LinkedHashMap<>();
            properties.put("connection-url", connector.url(db));
            properties.put("connection-user", config.at("/username").asText());
            properties.put("connection-password", config.at("/password").asText());
            String catalogName = databases.size() == 1 ? datasource.getName() : datasource.getName() + "_" + db;
            definitions.add(new CatalogDefinition(catalogName, connectorName, properties));
        }
        return definitions;
    }

    private List<CatalogDefinition> mongoCatalogDefinitions(Datasource datasource, JsonNode config, MongoDBConnector connector) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("mongodb.connection-url", connector.connectionUrl());
        return List.of(new CatalogDefinition(datasource.getName(), "mongodb", properties));
    }

    private List<CatalogDefinition> elasticsearchCatalogDefinitions(Datasource datasource, JsonNode config) {
        String uris = config.at("/uris").asText("");
        String[] hosts = DPUtil.explode(",", uris);
        if (hosts.length < 1 || DPUtil.empty(hosts[0])) {
            return List.of();
        }
        String host = hosts[0].trim();
        int port = 9200;
        String scheme = "http";
        if (host.contains("://")) {
            String[] schemeParts = host.split("://", 2);
            scheme = schemeParts[0];
            host = schemeParts[1];
        }
        if (host.contains(":")) {
            String[] hostPort = host.split(":", 2);
            host = hostPort[0];
            port = DPUtil.parseInt(hostPort[1]);
        }
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("elasticsearch.host", host);
        properties.put("elasticsearch.port", String.valueOf(port));
        properties.put("elasticsearch.default-schema-name", "default");
        String username = config.at("/username").asText("");
        String password = config.at("/password").asText("");
        if (!DPUtil.empty(username)) {
            properties.put("elasticsearch.security", "PASSWORD");
            properties.put("elasticsearch.auth.user", username);
            properties.put("elasticsearch.auth.password", password);
        }
        if ("https".equalsIgnoreCase(scheme)) {
            properties.put("elasticsearch.tls.enabled", "true");
        }
        properties.put("elasticsearch.ignore-publish-address", "true"); // 避免容器内网IP地址不可达
        return List.of(new CatalogDefinition(datasource.getName(), "fs_elasticsearch", properties));
    }

    private String trinoConnectorName(String type) {
        return switch (type) {
            case "mysql", "doris" -> "mysql";
            case "postgresql", "postgres" -> "postgresql";
            case "mongodb" -> "mongodb";
            case "elasticsearch" -> "elasticsearch";
            default -> type;
        };
    }

    private int dropAllCatalogs(MaintainEmitter emitter) throws SQLException {
        List<String> catalogs = catalogNames();
        List<String> pending = new ArrayList<>();
        for (String catalog : catalogs) {
            if ("system".equals(catalog)) {
                continue;
            }
            pending.add(catalog);
        }
        emitter.step("正在清理历史目录", "dropAllCatalogs", 5, pending.size());
        if (pending.isEmpty()) {
            emitter.log("没有需要清理的历史目录", "dropAllCatalogs", 5);
            return 0;
        }
        int dropped = 0;
        for (String catalog : pending) {
            execute("DROP CATALOG " + quoteIdentifier(catalog));
            dropped++;
            int percent = 5 + (int) Math.round(5.0 * dropped / pending.size());
            emitter.log("已删除目录 " + catalog + " (" + dropped + "/" + pending.size() + ")", "dropAllCatalogs", percent, "success");
        }
        return dropped;
    }

    private List<String> catalogNames() throws SQLException {
        List<String> catalogs = new ArrayList<>();
        query("SELECT catalog_name FROM system.metadata.catalogs", null, resultSet -> catalogs.add(resultSet.getString(1)));
        return catalogs;
    }

    private void createCatalog(CatalogDefinition definition) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE CATALOG ").append(quoteIdentifier(definition.name))
                .append(" USING ").append(definition.connector);
        if (!definition.properties.isEmpty()) {
            sql.append(" WITH (");
            int index = 0;
            for (Map.Entry<String, String> entry : definition.properties.entrySet()) {
                if (index++ > 0) {
                    sql.append(", ");
                }
                sql.append(quoteIdentifier(entry.getKey()))
                        .append(" = '")
                        .append(entry.getValue().replace("'", "''"))
                        .append("'");
            }
            sql.append(")");
        }
        execute(sql.toString());
    }

    public void execute(String sql) throws SQLException {
        try (Connection connection = trinoDataSource.getConnection();
            Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            statement.execute(sql);
        }
    }

    public String quoteIdentifier(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public void ensureDatasetSchema() throws SQLException {
        execute("CREATE SCHEMA IF NOT EXISTS "
                + quoteIdentifier(ICEBERG_CATALOG) + "." + quoteIdentifier(DATASET_SCHEMA));
    }

    public void dropDatasetSchema() throws SQLException {
        execute("DROP SCHEMA IF EXISTS "
                + quoteIdentifier(ICEBERG_CATALOG) + "." + quoteIdentifier(DATASET_SCHEMA) + " CASCADE");
    }

    public void createView(Dataset dataset, boolean bMaterialized) throws SQLException {
        String content = dataset.getContent();
        if (DPUtil.empty(content)) throw new SQLException("数据集查询SQL不能为空");
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR REPLACE ").append(bMaterialized ? "MATERIALIZED" : "").append(" VIEW ");
        sql.append(viewName(dataset.getName()));
        if (bMaterialized) {
            String location = "s3://" + DPUtil.trim(icebergBucket) + "/" + DATASET_SCHEMA + "/view-" + dataset.getId() + "/";
            String partitionClause = partitionClause(dataset.getPartitions());
            sql.append("\nWITH (\n  location = '").append(escapeSql(location)).append("'");
            if (!DPUtil.empty(partitionClause)) {
                sql.append(",\n").append(partitionClause);
            }
            sql.append("\n)");
        }
        sql.append("\nAS\n").append(content);
        execute(sql.toString());
    }

    public String refreshView(String name) throws SQLException {
        String sql = "REFRESH MATERIALIZED VIEW " + viewName(name);
        execute(sql);
        return sql;
    }

    public void dropView(String name, boolean bMaterialized) throws SQLException {
        if (DPUtil.empty(name)) return;
        execute("DROP " + (bMaterialized ? "MATERIALIZED" : "") + " VIEW IF EXISTS " + viewName(name));
    }

    private String viewName(String name) {
        return quoteIdentifier(ICEBERG_CATALOG) + "." + quoteIdentifier(DATASET_SCHEMA) + "." + quoteIdentifier(name);
    }

    private String partitionClause(String partitions) {
        List<String> values = new ArrayList<>();
        for (String item : DPUtil.parseStringList(partitions)) {
            String value = DPUtil.trim(item);
            if (!DPUtil.empty(value)) values.add(value);
        }
        if (values.isEmpty()) return "";
        StringBuilder sql = new StringBuilder("ARRAY[");
        boolean first = true;
        for (String value : values) {
            if (!first) sql.append(", ");
            sql.append("'").append(escapeSql(value)).append("'");
            first = false;
        }
        return "  partitioning = " + sql.append("]") + "\n";
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    public void query(String sql, StatementBinder binder, ResultSetConsumer consumer) throws SQLException {
        try (Connection connection = trinoDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (null != binder) {
                binder.bind(statement);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    consumer.accept(resultSet);
                }
            }
        }
    }

    /**
     * 获取数据集查询连接：默认上下文为内置 Iceberg 目录的 dataset Schema，
     * 便于数据集查询语句直接引用其他数据集名称。
     */
    public Connection connection() throws SQLException {
        Connection connection = trinoDataSource.getConnection();
        connection.setCatalog(ICEBERG_CATALOG);
        connection.setSchema(DATASET_SCHEMA);
        return connection;
    }

    public static class CatalogDefinition {
        private final String name;
        private final String connector;
        private final Map<String, String> properties;

        private CatalogDefinition(String name, String connector, Map<String, String> properties) {
            this.name = name;
            this.connector = connector;
            this.properties = properties;
        }
    }

    @FunctionalInterface
    public interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultSetConsumer {
        void accept(ResultSet resultSet) throws SQLException;
    }

}
