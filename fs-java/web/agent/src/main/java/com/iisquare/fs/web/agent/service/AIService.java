package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
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
import jakarta.annotation.PostConstruct;
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
    /** 请求配置在 @PostConstruct 里按注入后的配置项构建（构造阶段 @Value 还没注入） */
    RequestConfig config;
    /** 模型网关认证密钥：配置文件 fs.agent.token，整个 agent 服务共用 */
    @Value("${fs.agent.token:}")
    private String gatewayToken;
    @Value("${rpc.lm.rest}")
    private String gatewayEndpoint;
    /**
     * 模型网关调用超时：向量化、重排序这类接口在文档量大或模型排队时会慢，
     * 超时太短（原来固定 25s）会让知识检索频繁失败，因此放宽并允许配置；单位毫秒
     */
    @Value("${fs.agent.gateway.connectTimeout:5000}")
    private int gatewayConnectTimeout;
    @Value("${fs.agent.gateway.socketTimeout:300000}")
    private int gatewaySocketTimeout;
    @Value("${fs.agent.gateway.requestTimeout:10000}")
    private int gatewayRequestTimeout;

    public AIService() {
        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(200); // 最大连接数
        pooling.setDefaultMaxPerRoute(100); // 默认的每个路由的最大连接数

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManager(pooling);
        client = builder.build();
    }

    @PostConstruct
    public void initialize() {
        config = RequestConfig.custom()
                .setSocketTimeout(gatewaySocketTimeout)
                .setConnectTimeout(gatewayConnectTimeout)
                .setConnectionRequestTimeout(gatewayRequestTimeout)
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

    /**
     * 会话补全（一次性调用，非流式）：MCP 的多模态工具等按需分析场景用它调模型网关。
     * 认证与对话调用同一口径：服务级密钥 fs.agent.token，调用端带了会话令牌时一并透传
     * （x-auth-token），供网关按「用户代理」口径记账（密钥未开启代理时网关忽略该头）。
     */
    public Map<String, Object> chat(JsonNode body, String session) {
        Map<String, String> headers = authorization(gatewayToken);
        if (!DPUtil.empty(session)) headers.put(FeignInterceptor.HEADER_AUTH_TOKEN, session);
        return post(gatewayEndpoint + "/v1/chat/completions", body, headers);
    }

}
