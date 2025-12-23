package com.iisquare.fs.base.mongodb.config;

import com.iisquare.fs.base.core.util.FileUtil;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfiguration implements DisposableBean {

    private MongoClient client;

    @Value("${spring.mongodb.uri}")
    private String uri;
    @Value("${spring.mongodb.username}")
    private String username;
    @Value("${spring.mongodb.password}")
    private String password;
    @Value("${spring.mongodb.authSource:admin}")
    private String authSource;
    @Value("${spring.mongodb.minSize:0}")
    private Integer minSize;
    @Value("${spring.mongodb.maxSize:100}")
    private Integer maxSize;
    @Value("${spring.mongodb.maxWaitTime:1000}")
    private Long maxWaitTime;
    @Value("${spring.mongodb.connectTimeout:3000}")
    private Integer connectTimeout;
    @Value("${spring.mongodb.readTimeout:15000}")
    private Integer readTimeout;


    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    @Bean
    public MongoClient mongoClient() throws Exception {
        MongoCredential credential = MongoCredential.createScramSha256Credential(
                username, // 用户名
                authSource, // 认证数据库（authSource）
                password.toCharArray() // 密码
        );
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToConnectionPoolSettings(builder -> builder
                        .minSize(minSize) // 最小连接数
                        .maxSize(maxSize) // 最大连接数
                        .maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS) // 等待连接超时(毫秒)
                )
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS) // 连接超时
                        .readTimeout(readTimeout, TimeUnit.MILLISECONDS) // 读写超时
                )
                .credential(credential) // 设置认证凭证
                .applyConnectionString(new ConnectionString(uri))
                .build();
        MongoClient client = MongoClients.create(settings);
        return this.client = client;
    }

}
