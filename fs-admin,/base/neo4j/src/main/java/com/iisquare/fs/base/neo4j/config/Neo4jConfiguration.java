package com.iisquare.fs.base.neo4j.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class Neo4jConfiguration implements DisposableBean {

    @Value("${spring.neo4j.uri}")
    private String uri;
    @Value("${spring.neo4j.username:}")
    private String username;
    @Value("${spring.neo4j.password:}")
    private String password;
    @Value("${spring.neo4j.connectionTimeout:1000}")
    private Long connectionTimeout;
    @Value("${spring.neo4j.maxConnectionPoolSize:100}")
    private Integer maxConnectionPoolSize;
    private Driver driver;

    @Override
    public void destroy() throws Exception {
        if (null != driver) driver.close();
    }

    @Bean
    public Driver neo4jDriver() throws Exception {
        Config.ConfigBuilder builder = Config.builder();
        builder.withMaxConnectionPoolSize(maxConnectionPoolSize);
        builder.withConnectionTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
        return this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), builder.build());
    }

}
