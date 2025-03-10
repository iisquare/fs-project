package com.iisquare.fs.app.spark.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIHelper implements Closeable {

    final static String aiSite = System.getenv("AI_SITE");
    final static String aiToken = System.getenv("AI_TOKEN");
    final static String aiSystem = System.getenv("AI_SYSTEM");
    final static  String urlEmbedding = aiSite + "/v1/embeddings";
    final static  String urlCompletion = aiSite + "/v1/chat/completions";

    static Integer numbers = 512;

    CloseableHttpClient client;
    final RequestConfig config;

    public AIHelper() {
        SSLConnectionSocketFactory scsf;
        try {
            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(200); // 最大连接数
        pooling.setDefaultMaxPerRoute(100); // 默认的每个路由的最大连接数

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setSSLSocketFactory(scsf);
        builder.setConnectionManager(pooling);
        client = builder.build();
        config = RequestConfig.custom()
                .setSocketTimeout(600000)
                .setConnectTimeout(3000)
                .setConnectionRequestTimeout(5000)
                .build();
    }

    @Override
    public void close() {
        FileUtil.close(client);
    }

    public String post(String url, String params, Map<String, String> headers) {
        HttpPost http = new HttpPost(url);
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.addHeader(entry.getKey(), entry.getValue());
            }
        }
        StringEntity entity = new StringEntity(params, StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        http.setEntity(entity);
        return this.post(http);
    }

    public String post(HttpPost http) {
        CloseableHttpResponse response = null;
        try {
            http.setConfig(this.config);
            response = client.execute(http);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.close(response);
        }
    }

    public JsonNode vector(List<String> list) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization","Bearer " + aiToken);
        headers.put("Content-Type" , "application/json");
        ObjectNode params = DPUtil.objectNode();
        params.put("model" ,"bge-large-zh");
        params.replace("input", DPUtil.toJSON(list));
        String response = post(urlEmbedding, params.toString(), headers);
        return DPUtil.parseJSON(response).at("/data");
    }

    public String summary(String title, String content) {
        if (title.length() + content.length() < numbers) {
            return title + "\n" + content;
        }
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization","Bearer " + aiToken);
        headers.put("Content-Type" , "application/json");
        ObjectNode params = DPUtil.objectNode();
        params.put("model" ,"Qwen1.5-32B-Chat");
        ArrayNode messages = params.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", "<已知信息>\n" + title + "\n" + content
                + "\n</已知信息>\n<指令>根据已知信息，使用一段话简单明了的概括全文，要求在500字以内。 </指令>");
        params.put("temperature" , 0.7);
        params.put("stream" , false);
        String response = this.post(urlCompletion, params.toString(), headers);
        JsonNode json = DPUtil.parseJSON(response);
        JsonNode choices = json.at("/choices");
        if (choices.size() == 0) {
            return title + "\n" + content;
        }
        return choices.get(0).at("/message/content").asText();
    }

    public String qa(String question) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization","Bearer " + aiToken);
        headers.put("Content-Type" , "application/json");
        ObjectNode params = DPUtil.objectNode();
        params.put("model" ,"Qwen1.5-32B-Chat");
        ArrayNode messages = params.putArray("messages");
        if (!DPUtil.empty(aiSystem)) {
            messages.addObject().put("role", "system").put("content", aiSystem);
        }
        messages.addObject().put("role", "user").put("content", question);
        params.put("temperature" , 0.7);
        params.put("stream" , false);
        String response = this.post(urlCompletion, params.toString(), headers);
        JsonNode json = DPUtil.parseJSON(response);
        JsonNode choices = json.at("/choices");
        return choices.get(0).at("/message/content").asText();
    }

}
