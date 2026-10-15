package com.iisquare.fs.base.core.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 JDK HttpClient 的轻量级 REST 调用工具。
 * 用于访问 Flink REST、Spark 提交服务等外部集群接口，支持 GET/POST/PUT/PATCH/DELETE 与 multipart 上传。
 */
public class RestUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static class Response {
        private final int status;
        private final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = null == body ? "" : body;
        }

        public int status() {
            return status;
        }

        public String body() {
            return body;
        }

        public boolean succeed() {
            return status >= 200 && status < 300;
        }

        public JsonNode json() {
            return DPUtil.parseJSON(body);
        }

        @Override
        public String toString() {
            return "status=" + status + ", body=" + body;
        }
    }

    public static Map<String, String> jsonHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        return headers;
    }

    public static Response request(String method, String url, Map<String, String> headers, String body, long timeoutSeconds) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds));
            if (null != headers) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }
            byte[] data = null == body ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofByteArray(data);
            switch (method.toUpperCase()) {
                case "GET":
                    builder.GET();
                    break;
                case "POST":
                    builder.POST(publisher);
                    break;
                case "PUT":
                    builder.PUT(publisher);
                    break;
                case "PATCH":
                    builder.method("PATCH", publisher);
                    break;
                case "DELETE":
                    builder.method("DELETE", publisher);
                    break;
                default:
                    return new Response(-1, "不支持的请求方法：" + method);
            }
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(response.statusCode(), response.body());
        } catch (Exception e) {
            return new Response(-1, e.getClass().getSimpleName() + "：" + e.getMessage());
        }
    }

    public static Response request(String method, String url) {
        return request(method, url, null, null, 30);
    }

    public static Response get(String url) {
        return request("GET", url, null, null, 30);
    }

    public static Response postJson(String url, String json) {
        return request("POST", url, jsonHeaders(), json, 60);
    }

    public static Response patchJson(String url, String json) {
        return request("PATCH", url, jsonHeaders(), null == json ? "" : json, 60);
    }

    public static Response putJson(String url, String json) {
        return request("PUT", url, jsonHeaders(), json, 60);
    }

    /**
     * multipart/form-data 文件上传，用于Flink上传作业Jar包
     */
    public static Response upload(String url, String fieldName, String fileName, byte[] content, long timeoutSeconds) {
        try {
            String boundary = "----FsProjectBoundary" + System.currentTimeMillis();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: application/java-archive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(response.statusCode(), response.body());
        } catch (Exception e) {
            return new Response(-1, e.getClass().getSimpleName() + "：" + e.getMessage());
        }
    }

}
