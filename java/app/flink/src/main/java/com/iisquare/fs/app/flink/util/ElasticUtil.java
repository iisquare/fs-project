package com.iisquare.fs.app.flink.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.streaming.connectors.elasticsearch7.RestClientFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.util.ArrayList;
import java.util.List;

public class ElasticUtil {

    public static List<HttpHost> hosts(String nodes) {
        List<HttpHost> hosts = new ArrayList<>();
        for (String target : DPUtil.explode(nodes, ",", " ", true)) {
            String[] strings = DPUtil.explode(target, ":", " ", true);
            String host = strings.length > 0 ? strings[0] : "localhost";
            int port = strings.length > 1 ? DPUtil.parseInt(strings[1]) : 9200;
            hosts.add(new HttpHost(host, port, "http"));
        }
        if (hosts.size() < 1) hosts.add(new HttpHost("localhost", 9200, "http"));
        return hosts;
    }

    public static RestClientFactory client(int maxConnTotal, int maxConnPerRoute, String username, String password) {
        return (RestClientFactory) restClientBuilder -> {
            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder.setMaxConnTotal(maxConnTotal);
                httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
                if (DPUtil.empty(username)) return httpClientBuilder;
                httpClientBuilder.disableAuthCaching();
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            });
        };
    }

}
