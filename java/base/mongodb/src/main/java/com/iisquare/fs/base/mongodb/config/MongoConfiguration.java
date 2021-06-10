package com.iisquare.fs.base.mongodb.config;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfiguration implements DisposableBean {

    private MongoClient client;

    @Value("${spring.mongodb.hosts}")
    private String hosts;
    @Value("${spring.mongodb.database}")
    private String database;
    @Value("${spring.mongodb.username}")
    private String username;
    @Value("${spring.mongodb.password}")
    private String password;

    @Value("${spring.mongodb.connectionsPerHost:100}")
    private Integer connectionsPerHost;
    @Value("${spring.mongodb.connectTimeout:1000}")
    private Integer connectTimeout;
    @Value("${spring.mongodb.maxWaitTime:60000}")
    private Integer maxWaitTime;
    @Value("${spring.mongodb.socketTimeout:0}")
    private Integer socketTimeout;

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    @Bean
    public MongoClient afterPropertiesSet() throws Exception {
        MongoCredential credential = MongoCredential.createCredential(username, database, password.toCharArray());
        String[] hostArray = DPUtil.explode(hosts, ",", " ", true);
        List<ServerAddress> list = new ArrayList<>();
        for (String item : hostArray) {
            String[] items = DPUtil.explode(item, ":", " ", true);
            int port = DPUtil.parseInt(items.length > 1 ? items[1] : 27017);
            list.add(new ServerAddress(items[0], port));
        }
        MongoClientOptions.Builder options = new MongoClientOptions.Builder();
        options.connectionsPerHost(connectionsPerHost);
        options.connectTimeout(connectTimeout);
        options.maxWaitTime(maxWaitTime);
        options.socketTimeout(socketTimeout);
        MongoClient client = new MongoClient(list, credential, options.build());
        client.listDatabaseNames(); // trigger connection
        return this.client = client;
    }

}
