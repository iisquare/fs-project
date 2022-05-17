package com.iisquare.fs.base.elasticsearch.config;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ElasticsearchConfiguration implements DisposableBean {

    @Value("${spring.elasticsearch.nodes}")
    private String nodes;
    @Value("${spring.elasticsearch.rest.username:}")
    private String username;
    @Value("${spring.elasticsearch.rest.password:}")
    private String password;
    @Value("${spring.elasticsearch.rest.connectionTimeout:1000}")
    private Integer connectionTimeout;
    @Value("${spring.elasticsearch.rest.readTimeout:30000}")
    private Integer readTimeout;
    @Value("${spring.elasticsearch.rest.maxConnPerRoute:10}")
    private Integer maxConnPerRoute;
    @Value("${spring.elasticsearch.rest.maxConnTotal:30}")
    private Integer maxConnTotal;
    private RestHighLevelClient client;

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    @Bean
    public RestHighLevelClient afterPropertiesSet() throws Exception {
        List<HttpHost> hosts = new ArrayList<>();
        for (String target : DPUtil.explode(",", nodes, " ", true)) {
            String[] strings = DPUtil.explode(":", target, " ", true);
            String host = strings.length > 0 ? strings[0] : "localhost";
            int port = strings.length > 1 ? DPUtil.parseInt(strings[1]) : 9200;
            hosts.add(new HttpHost(host, port, "http"));
        }
        if (hosts.size() < 1) hosts.add(new HttpHost("localhost", 9200, "http"));
        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(maxConnTotal);
            httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
            if (DPUtil.empty(username)) return httpClientBuilder;
            httpClientBuilder.disableAuthCaching();
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        });
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(connectionTimeout);
            requestConfigBuilder.setSocketTimeout(readTimeout);
            return requestConfigBuilder;
        });
        return this.client = new RestHighLevelClient(builder);
    }

}
