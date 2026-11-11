package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 配置信息：{
 *     "uri": "连接地址",
 *     "username": "用户名",
 *     "password": "密码",
 *     "authSource": "认证库",
 *     "connectTimeout": "连接超时(ms)",
 *     "readTimeout": "读取超时(ms)",
 *     "minSize": "最小连接池",
 *     "maxSize": "最大连接池",
 *     "maxWaitTime": "最大等待时间(ms)"
 * }
 */
public class MongoDBConnector extends DatasourceConnector<MongoClient> {

    public MongoDBConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public String summary() {
        return config.at("/uri").asText();
    }

    public String connectionUrl() {
        String uri = config.at("/uri").asText("");
        if (DPUtil.empty(uri)) {
            uri = "mongodb://localhost:27017";
        }
        String database = DPUtil.trim(config.at("/database").asText(""));
        if (!DPUtil.empty(database)) uri = withDatabase(uri, database);
        String username = config.at("/username").asText("");
        String password = config.at("/password").asText("");
        if (DPUtil.empty(username) || uri.contains("@")) {
            return uri;
        }
        String authSource = config.at("/authSource").asText("admin");
        String scheme = uri.startsWith("mongodb+srv://") ? "mongodb+srv://" : "mongodb://";
        String rest = uri.substring(scheme.length());
        int slash = rest.indexOf('/');
        String hostPort = slash < 0 ? rest : rest.substring(0, slash);
        String suffix = slash < 0 ? "/" : rest.substring(slash);
        if (!suffix.contains("?")) {
            if (!suffix.endsWith("/")) {
                suffix += "/";
            }
        }
        if (!suffix.contains("authSource=")) {
            suffix += suffix.contains("?") ? "&authSource=" : "?authSource=";
            suffix += encode(authSource);
        }
        return scheme + encode(username) + ":" + encode(password) + "@" + hostPort + suffix;
    }

    /**
     * Trino MongoDB 目录用于展示整个实例时不能带默认数据库，否则元数据只会暴露该库。
     */
    public String catalogConnectionUrl() {
        String uri = connectionUrl();
        return DPUtil.empty(config.at("/database").asText("")) ? withoutDatabase(uri) : uri;
    }

    private String withDatabase(String uri, String database) {
        int scheme = uri.indexOf("://");
        if (scheme < 0) return uri;
        int authorityStart = scheme + 3;
        int query = uri.indexOf('?', authorityStart);
        int path = uri.indexOf('/', authorityStart);
        String encoded = encode(database);
        if (path >= 0 && (query < 0 || path < query)) {
            return uri.substring(0, path + 1) + encoded + (query < 0 ? "" : uri.substring(query));
        }
        String before = query < 0 ? uri : uri.substring(0, query);
        String after = query < 0 ? "" : uri.substring(query);
        if (!before.endsWith("/")) before += "/";
        return before + encoded + after;
    }

    private String withoutDatabase(String uri) {
        int scheme = uri.indexOf("://");
        if (scheme < 0) return uri;
        int authorityStart = scheme + 3;
        int query = uri.indexOf('?', authorityStart);
        int path = uri.indexOf('/', authorityStart);
        if (path < 0) {
            return query < 0 ? uri : uri.substring(0, query) + "/" + uri.substring(query);
        }
        if (query >= 0 && path > query) {
            return uri.substring(0, query) + "/" + uri.substring(query);
        }
        return uri.substring(0, path + 1) + (query < 0 ? "" : uri.substring(query));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    public MongoClient open() throws Exception {
        String uri = config.at("/uri").asText();
        String username = config.at("/username").asText();
        String password = config.at("/password").asText();
        String authSource = config.at("/authSource").asText("admin");
        int connectTimeout = config.at("/connectTimeout").asInt(3000);
        int readTimeout = config.at("/readTimeout").asInt(15000);
        int minSize = config.at("/minSize").asInt(0);
        int maxSize = config.at("/maxSize").asInt(100);
        long maxWaitTime = config.at("/maxWaitTime").asLong(1000);
        if (DPUtil.empty(uri)) {
            uri = "mongodb://localhost:27017";
        }
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToConnectionPoolSettings(builder -> builder
                        .minSize(minSize)
                        .maxSize(maxSize)
                        .maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeout, TimeUnit.MILLISECONDS));
        if (!DPUtil.empty(username)) {
            MongoCredential credential = MongoCredential.createScramSha256Credential(
                    username, authSource, password.toCharArray());
            settingsBuilder.credential(credential);
        }
        return MongoClients.create(settingsBuilder.build());
    }

    @Override
    public void close(MongoClient client) {
        FileUtil.close(client);
    }

    @Override
    public Map<String, Object> test() {
        MongoClient client = null;
        try {
            client = open();
            String database = config.at("/database").asText("admin");
            MongoDatabase db = client.getDatabase(database);
            return ApiUtil.result(0, "连接成功", db.runCommand(new Document("ping", 1)).toJson());
        } catch (Exception e) {
            return ApiUtil.result(1500, "连接失败", e.getMessage());
        } finally {
            close(client);
        }
    }

}
