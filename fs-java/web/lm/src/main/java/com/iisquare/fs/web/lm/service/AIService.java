package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService implements DisposableBean {

    CloseableHttpClient client;
    final RequestConfig config;
    @Value("${fs.lm.token}")
    private String gatewayToken;
    @Value("${rpc.lm.rest}")
    private String gatewayEndpoint;

    public AIService() {
        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(200); // 最大连接数
        pooling.setDefaultMaxPerRoute(100); // 默认的每个路由的最大连接数

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManager(pooling);
        client = builder.build();
        config = RequestConfig.custom()
                .setSocketTimeout(25000)
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(3000)
                .build();
    }

    public Map<String, Object> get(String url, Map<String, String> headers) {
        HttpGet http = new HttpGet(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return this.execute(http);
    }

    public Map<String, Object> post(String url, JsonNode json, Map<String, String> headers) {
        return post(url, json, headers, 0);
    }

    /**
     * 发起JSON请求，可单独指定读取超时（毫秒）。
     * 网页抓取需要访问目标站点，耗时通常高于模型补全接口，因此允许调用方放宽超时。
     */
    public Map<String, Object> post(String url, JsonNode json, Map<String, String> headers, int socketTimeout) {
        HttpPost http = new HttpPost(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.addHeader(entry.getKey(), entry.getValue());
            }
        }
        String body = DPUtil.stringify(json);
        if (null == body) return ApiUtil.result(10500, "转换模型请求参数失败", json);
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        http.setEntity(entity);
        if (socketTimeout > 0) {
            return this.execute(http, RequestConfig.copy(config).setSocketTimeout(socketTimeout).build());
        }
        return this.execute(http);
    }

    public Map<String, Object> execute(HttpRequestBase http) {
        return execute(http, config);
    }

    public Map<String, Object> execute(HttpRequestBase http, RequestConfig requestConfig) {
        CloseableHttpResponse response = null;
        try {
            http.setConfig(requestConfig);
            response = client.execute(http);
            int code = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (200 != code) {
                return ApiUtil.result(15000 + code, "模型接口响应状态异常", result);
            }
            JsonNode json = DPUtil.parseJSON(result);
            if (null == json) {
                return ApiUtil.result(10501, "解析模型返回结果失败", result);
            }
            return ApiUtil.result(0, null, json);
        } catch (Exception e) {
            return ApiUtil.result(10503, "请求模型接口失败", e.getMessage());
        } finally {
            FileUtil.close(response);
        }
    }

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    public Map<String, String> authorization(String token) {
        Map<String, String> headers = new HashMap<>();
        if (!DPUtil.empty(token)) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    public Map<String, Object> models() {
        String url = gatewayEndpoint + "/v1/models";
        return get(url, authorization(gatewayToken));
    }

    public Map<String, Object> embeddings(String model, List<String> inputs) {
        String url = gatewayEndpoint + "/v1/embeddings";
        ObjectNode body = DPUtil.objectNode();
        body.put("model", model);
        body.replace("input", DPUtil.toJSON(inputs));
        return post(url, body, authorization(gatewayToken));
    }

    public Map<String, Object> rerank(String model, String query, List<String> documents, int topN) {
        String url = gatewayEndpoint + "/v1/rerank";
        ObjectNode body = DPUtil.objectNode();
        body.put("model", model);
        body.put("query", query);
        body.replace("documents", DPUtil.toJSON(documents));
        body.put("return_documents", true);
        if (topN > 0) body.put("top_n", topN);
        return post(url, body, authorization(gatewayToken));
    }

}
