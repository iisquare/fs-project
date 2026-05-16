package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService implements DisposableBean {

    CloseableHttpClient client;
    final RequestConfig config;

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

    public Map<String, Object> post(String url, JsonNode json, Map<String, String> headers) {
        HttpPost http = new HttpPost(url);
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.addHeader(entry.getKey(), entry.getValue());
            }
        }
        String body = DPUtil.stringify(json);
        if (null == body) return ApiUtil.result(10500, "转换模型请求参数失败", json);
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        http.setEntity(entity);
        return this.execute(http);
    }

    public Map<String, Object> execute(HttpRequestBase http) {
        CloseableHttpResponse response = null;
        try {
            http.setConfig(this.config);
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
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }

}
