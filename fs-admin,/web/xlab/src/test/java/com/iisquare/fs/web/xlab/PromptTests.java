package com.iisquare.fs.web.xlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class PromptTests {

    CloseableHttpClient client;

    final RequestConfig config = RequestConfig.custom()
            .setSocketTimeout(150000).setConnectTimeout(15000).setConnectionRequestTimeout(15000).build();

    final static String charset = "UTF-8";

    final static  String url = "http://59.53.171.165:30777/v1/chat/completions";

    final Map<String, String> headers = new LinkedHashMap<String, String>(){{
        put("Authorization","Bearer xxx");
        put("Content-Type" , "application/json");
    }};

    @Before
    public void init() {
        this.client = HttpClients.custom().build();
    }

    @After
    public void destroy() {
        FileUtil.close(client);
    }

    public String post(String url, String data, Map<String, String> headers) throws Exception {
        HttpPost request = new HttpPost(url);
        if (!DPUtil.empty(data)) {
            StringEntity entity = new StringEntity(data, charset);
            entity.setContentType("application/json");
            request.setEntity(entity);
        }
        if (null != headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        request.setConfig(config);
        CloseableHttpResponse response = this.client.execute(request);
        String content = EntityUtils.toString(response.getEntity(), charset);
        FileUtil.close(response);
        return content;
    }

    public void chat(ArrayNode messages) throws Exception {
        ObjectNode data = DPUtil.objectNode();
        data.put("model" ,"baichuan2-13b-chat" );
        data.replace("messages", messages);
        data.put("temperature" , 0.7);
        data.put("stream" , false);
        String content = post(url, DPUtil.stringify(data), headers);
        JsonNode json = DPUtil.parseJSON(content);
        if (null != json) {
            System.out.println(json.toPrettyString());
        }
    }

    @Test
    public void prompt() throws Exception {
        String sys = "";
        String question = "介绍下自己";
        ArrayNode messages = DPUtil.arrayNode();
        messages.addObject().put("role", "system").put("content", sys);
        messages.addObject().put("role", "user").put("content", question);
        chat(messages);
    }

}
