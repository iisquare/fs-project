package com.iisquare.fs.web.bi.datasource;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.elasticsearch.util.ElasticsearchUtil;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 配置信息：{
 *     "uris": "地址",
 *     "username": "用户名",
 *     "password": "密码"
 * }
 */
public class ElasticsearchConnector extends DatasourceConnector<ElasticsearchClient> {

    public ElasticsearchConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public String summary() {
        return config.at("/uris").asText();
    }

    @Override
    public ElasticsearchClient open() throws Exception {
        String uris = config.at("/uris").asText();
        String username = config.at("/username").asText();
        String password = config.at("/password").asText();
        List<HttpHost> hosts = new ArrayList<>();
        for (String target : DPUtil.explode(",", uris)) {
            hosts.add(HttpHost.create(target.trim()));
        }
        if (hosts.isEmpty()) hosts.add(new HttpHost("localhost", 9200, "http"));
        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (DPUtil.empty(username)) return httpClientBuilder;
            httpClientBuilder.disableAuthCaching();
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        });
        RestClientTransport transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    @Override
    public void close(ElasticsearchClient client) {
        FileUtil.close(client);
    }

    /**
     * 8.x Java API Client 的传输层（ElasticsearchTransportBase）对每个高层 API 响应都会
     * 强制校验 X-Elastic-Product: Elasticsearch 响应头，且该校验无法通过配置关闭。
     * 使用client.info()校验ES 7.x会抛出 Missing [X-Elastic-Product] header。
     * 底层 RestClient 不做该产品头校验，可同时兼容 ES 7.x 与 8.x。
     */
    @Override
    public Map<String, Object> test() {
        ElasticsearchClient client = null;
        try {
            client = open();
            Response response = ElasticsearchUtil.rest(client).performRequest(new Request("GET", "/"));
            int status = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                return ApiUtil.result(1500, "连接失败", "HTTP " + status + " - " + body);
            }
            JsonNode json = DPUtil.parseJSON(body);
            if (null == json) {
                return ApiUtil.result(1500, "响应结果解析失败", body);
            }
            return ApiUtil.result(0, "连接成功", json);
        } catch (Exception e) {
            return ApiUtil.result(1500, "连接失败", e.getMessage());
        } finally {
            close(client);
        }
    }

}
