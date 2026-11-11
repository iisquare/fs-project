package com.iisquare.fs.app.spark.helper;

import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.neo4j.driver.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Neo4jHelper implements Closeable {

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public Neo4jHelper(Map<String, String> config) {
        Config.ConfigBuilder builder = Config.builder();
        builder.withMaxConnectionPoolSize(positive(config.get("maxSize"), 20));
        builder.withConnectionTimeout(positive(config.get("connectTimeout"), 3000),
                TimeUnit.MILLISECONDS);
        builder.withMaxConnectionLifetime(positive(config.get("maxConnectionLifetime"), 3600000),
                TimeUnit.MILLISECONDS);
        String username = DPUtil.parseString(config.get("username"));
        AuthToken token = DPUtil.empty(username)
                ? AuthTokens.none()
                : AuthTokens.basic(username, DPUtil.parseString(config.get("password")));
        driver = GraphDatabase.driver(config.get("uri"), token, builder.build());
        String database = DPUtil.parseString(config.get("database"));
        sessionConfig = DPUtil.empty(database)
                ? SessionConfig.defaultConfig()
                : SessionConfig.forDatabase(database);
    }

    public Session session() {
        return driver.session(sessionConfig);
    }

    public Result run(String cql) {
        try (Session session = session()) {
            return session.run(cql);
        }
    }

    public Result run(String cql, Map<String,Object> parameters) {
        try (Session session = session()) {
            return session.run(cql, parameters);
        }
    }

    public void execute(String cql, Map<String, Object> parameters) {
        try (Session session = session()) {
            session.run(cql, parameters).consume();
        }
    }

    private static int positive(Object value, int defaultValue) {
        int result = DPUtil.parseInt(value);
        return result > 0 ? result : defaultValue;
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(driver);
    }
}
