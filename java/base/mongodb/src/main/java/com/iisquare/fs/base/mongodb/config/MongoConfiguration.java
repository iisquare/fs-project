package com.iisquare.fs.base.mongodb.config;

import com.iisquare.fs.base.core.util.FileUtil;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfiguration implements DisposableBean {

    private MongoClient client;

    @Value("${spring.mongodb.uri}")
    private String uri;

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    @Bean
    public MongoClient mongoClient() throws Exception {
        MongoClient client = MongoClients.create(uri);
        return this.client = client;
    }

}
