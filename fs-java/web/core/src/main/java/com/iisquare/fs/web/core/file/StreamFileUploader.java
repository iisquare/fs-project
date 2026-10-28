package com.iisquare.fs.web.core.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * 文件服务流式上传器。
 *
 * Feign 的 multipart 编码会在代理层重新组装请求，大文件通过 fallback 时容易出现连接中断。
 * 这里直接用 fixed-length streaming 把 MultipartFile 写入文件服务，文件内容不进入堆内存。
 */
@Service
public class StreamFileUploader {

    @Value("${rpc.file.rest}")
    private String endpoint;
    @Value("${fs.rpc.upload.connectTimeout:5000}")
    private int connectTimeout;
    @Value("${fs.rpc.upload.readTimeout:900000}")
    private int readTimeout;

    @Autowired
    Environment environment;

    public Map<String, Object> upload(String uri, Map<?, ?> fields, MultipartFile file) {
        if (null == file) return ApiUtil.result(1003, "获取文件句柄失败", null);
        return upload(uri, fields, file, file.getOriginalFilename(), file.getContentType());
    }

    /** 允许调用方只覆盖 multipart 中的文件名 / MIME，不复制文件内容。 */
    public Map<String, Object> upload(String uri, Map<?, ?> fields, MultipartFile file,
            String filename, String contentType) {
        if (null == file) return ApiUtil.result(1003, "获取文件句柄失败", null);
        HttpURLConnection connection = null;
        try {
            String boundary = "----fs-upload-" + UUID.randomUUID();
            byte[] prefix = multipartPrefix(boundary, fields, filename, contentType);
            byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            long totalLength = (long) prefix.length + file.getSize() + suffix.length;
            URL url = new URL(endpoint.replaceAll("/+$", "") + uri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setFixedLengthStreamingMode(totalLength);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");
            HttpServletRequest request = currentRequest();
            for (Map.Entry<String, String> entry : FeignInterceptor.headers(request, environment).entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            try (OutputStream output = connection.getOutputStream();
                 InputStream input = file.getInputStream()) {
                output.write(prefix);
                byte[] buffer = new byte[64 * 1024];
                int length;
                while ((length = input.read(buffer)) >= 0) {
                    if (length > 0) output.write(buffer, 0, length);
                }
                output.write(suffix);
            }
            int status = connection.getResponseCode();
            InputStream response = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            if (null == response) {
                return ApiUtil.result(1500 + status, "文件服务未返回内容", status);
            }
            String body;
            try (InputStream input = response) {
                body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonNode json = DPUtil.parseJSON(body);
            if (null == json) {
                return ApiUtil.result(1500 + status, "文件服务响应格式异常", body);
            }
            // 保持与 Feign 接口一致的 data 节点类型：业务层仍按 ObjectNode 读取
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(ApiUtil.FIELD_CODE, json.at("/code").asInt(status));
            result.put(ApiUtil.FIELD_MSG, json.at("/message").asText(""));
            result.put(ApiUtil.FIELD_DATA, json.get(ApiUtil.FIELD_DATA));
            return result;
        } catch (Exception e) {
            return ApiUtil.result(1502, "上传文件到文件服务失败", e.getMessage());
        } finally {
            if (null != connection) connection.disconnect();
        }
    }

    protected byte[] multipartPrefix(String boundary, Map<?, ?> fields,
            String originalFilename, String originalContentType)
            throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (Map.Entry<?, ?> entry : fields.entrySet()) {
            String name = DPUtil.parseString(entry.getKey());
            if (DPUtil.empty(name)) continue;
            write(output, "--" + boundary + "\r\n");
            write(output, "Content-Disposition: form-data; name=\"" + safe(name) + "\"\r\n\r\n");
            write(output, DPUtil.parseString(entry.getValue()) + "\r\n");
        }
        String filename = safe(DPUtil.parseString(originalFilename));
        if (DPUtil.empty(filename)) filename = "file";
        String contentType = DPUtil.parseString(originalContentType);
        if (DPUtil.empty(contentType)) contentType = "application/octet-stream";
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"\r\n");
        write(output, "Content-Type: " + safe(contentType) + "\r\n\r\n");
        return output.toByteArray();
    }

    protected void write(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    protected String safe(String value) {
        return value.replace("\r", "").replace("\n", "").replace("\"", "'");
    }

    protected HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return null == attributes ? null : attributes.getRequest();
    }

}
