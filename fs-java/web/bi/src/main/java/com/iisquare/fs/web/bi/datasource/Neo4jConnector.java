package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 配置信息：{
 *     "uri": "neo4j://host:7687",
 *     "username": "用户名",
 *     "password": "密码",
 *     "database": "数据库，默认由服务端决定",
 *     "connectTimeout": "连接超时(ms)"
 * }
 */
public class Neo4jConnector extends DatasourceConnector<Driver> {

    public Neo4jConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public String summary() {
        String uri = uri();
        String database = database();
        return DPUtil.empty(database) ? uri : uri + " · " + database;
    }

    public String uri() {
        String uri = config.at("/uri").asText("");
        if (DPUtil.empty(uri)) uri = config.at("/url").asText("");
        return DPUtil.empty(uri) ? "neo4j://localhost:7687" : uri;
    }

    public String database() {
        return DPUtil.trim(config.at("/database").asText(""));
    }

    public SessionConfig sessionConfig() {
        String database = database();
        return DPUtil.empty(database) ? SessionConfig.defaultConfig() : SessionConfig.forDatabase(database);
    }

    @Override
    public Driver open() throws Exception {
        String username = config.at("/username").asText("");
        String password = config.at("/password").asText("");
        int connectTimeout = config.at("/connectTimeout").asInt(3000);
        int maxLifetime = config.at("/maxConnectionLifetime").asInt(3600000);
        Config.ConfigBuilder builder = Config.builder()
                .withConnectionTimeout(Math.max(1, connectTimeout), TimeUnit.MILLISECONDS)
                .withMaxConnectionLifetime(Math.max(1, maxLifetime), TimeUnit.MILLISECONDS)
                .withMaxConnectionPoolSize(Math.max(1, config.at("/maxSize").asInt(20)));
        if (DPUtil.empty(username)) {
            return GraphDatabase.driver(uri(), builder.build());
        }
        return GraphDatabase.driver(uri(), AuthTokens.basic(username, password), builder.build());
    }

    @Override
    public void close(Driver driver) {
        FileUtil.close(driver);
    }

    @Override
    public Map<String, Object> test() {
        Driver driver = null;
        try {
            driver = open();
            driver.verifyConnectivity();
            String database = database();
            try (var session = driver.session(sessionConfig())) {
                var record = session.run("RETURN 1 AS ok").single();
                return ApiUtil.result(0, "连接成功",
                        DPUtil.buildMap("uri", uri(), "database", database, "ok", record.get("ok").asInt()));
            }
        } catch (Exception e) {
            return ApiUtil.result(1500, "连接失败", e.getMessage());
        } finally {
            close(driver);
        }
    }
}
