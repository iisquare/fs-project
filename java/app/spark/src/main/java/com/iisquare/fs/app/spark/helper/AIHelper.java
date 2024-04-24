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
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIHelper implements Closeable {

    final static String aiSite = System.getenv("AI_SITE");
    final static String aiToken = System.getenv("AI_TOKEN");
    final static  String urlEmbedding = aiSite + "/v1/embeddings";
    final static  String urlCompletion = aiSite + "/v1/chat/completions";

    static Integer numbers = 512;

    CloseableHttpClient client;
    final RequestConfig config;

    public AIHelper() {
        client = HttpClients.custom().build();
        config = RequestConfig.custom()
                .setSocketTimeout(150000)
                .setConnectTimeout(15000)
                .setConnectionRequestTimeout(15000)
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
            return null;
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

}
