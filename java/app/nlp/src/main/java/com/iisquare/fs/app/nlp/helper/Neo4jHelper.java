package com.iisquare.fs.app.nlp.helper;

import com.iisquare.fs.base.core.util.FileUtil;
import org.neo4j.driver.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Neo4jHelper implements Closeable {

    private final Driver driver;

    public Neo4jHelper(Map<String, String> config) {
        Config.ConfigBuilder builder = Config.builder();
        builder.withMaxConnectionPoolSize(1);
        builder.withConnectionTimeout(3000, TimeUnit.MILLISECONDS);
        driver = GraphDatabase.driver(config.get("uri"), AuthTokens.basic(config.get("username"), config.get("password")), builder.build());
    }

    public Result run(String cql) {
        try (Session session = driver.session()) {
            return session.run(cql);
        }
    }

    public Result run(String cql, Map<String,Object> parameters) {
        try (Session session = driver.session()) {
            return session.run(cql, parameters);
        }
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(driver);
    }
}
