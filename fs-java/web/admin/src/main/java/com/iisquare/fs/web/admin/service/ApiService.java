package com.iisquare.fs.web.admin.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class ApiService implements DisposableBean {

    @Autowired
    WebApplicationContext context;

    private final CloseableHttpClient client;

    public static final List<String> FORWARD_HEADERS = Arrays.asList(
            "x-auth-token", "cookie", "user-agent", "authorization",
            "content-disposition", "range", "accept", "accept-encoding"
    );

    public ApiService() {
        SSLConnectionSocketFactory scsf;
        try {
            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(20000);
        pooling.setDefaultMaxPerRoute(1000);
        this.client = HttpClientBuilder.create()
                .setSSLSocketFactory(scsf)
                .setConnectionManager(pooling)
                .build();
    }

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    /**
     * 从 /api/{appName}/** 中提取 ** 部分
     */
    public String extractPath(HttpServletRequest request) {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatch = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return new AntPathMatcher().extractPathWithinPattern(bestMatch, fullPath);
    }

    public String resolveBackendUrl(String appName) {
        String key = String.format("rpc.%s.rest", appName).toLowerCase();
        return context.getEnvironment().getProperty(key);
    }

    public String buildUrl(String backendUrl, String appPath, String queryString) {
        StringBuilder sb = new StringBuilder(backendUrl);
        if (!backendUrl.endsWith("/")) sb.append("/");
        if (appPath.startsWith("/")) appPath = appPath.substring(1);
        sb.append(appPath);
        if (!DPUtil.empty(queryString)) sb.append("?").append(queryString);
        return sb.toString();
    }

    /**
     * 根据HTTP方法构建后端请求
     */
    public HttpRequestBase buildBackendRequest(String fullUrl, HttpServletRequest request) throws IOException {
        String method = request.getMethod();
        switch (method.toUpperCase()) {
            case "GET":
                return new HttpGet(fullUrl);
            case "DELETE":
                return new HttpDelete(fullUrl);
            case "POST":
            case "PUT":
            case "PATCH": {
                HttpEntityEnclosingRequestBase entityRequest = switch (method.toUpperCase()) {
                    case "POST" -> new HttpPost(fullUrl);
                    case "PUT" -> new HttpPut(fullUrl);
                    default -> new HttpPatch(fullUrl);
                };
                // 原始流透传请求体（JSON/form/multipart/binary 统一处理）
                InputStream is = request.getInputStream();
                byte[] body = IOUtils.toByteArray(is);
                if (body.length > 0) {
                    entityRequest.setEntity(new InputStreamEntity(
                            new ByteArrayInputStream(body), body.length));
                }
                return entityRequest;
            }
            default:
                return null;
        }
    }

    /**
     * 转发HTTP头到后端
     */
    public void applyHeaders(HttpRequestBase httpRequest, HttpServletRequest request) {
        for (String header : FORWARD_HEADERS) {
            String value = request.getHeader(header);
            if (!DPUtil.empty(value)) {
                httpRequest.addHeader(header, value);
            }
        }
        // 透传 Content-Type（multipart 的 boundary 原样保留）
        String contentType = request.getContentType();
        if (contentType != null) {
            httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
        }
        // 调用端标识
        httpRequest.addHeader(FeignInterceptor.HEADER_APP_NAME, "fs-admin-service");
    }

    /**
     * 从 spring.cloud.openfeign.client.config 读取超时配置
     * 优先查找服务级别配置，回退到 default
     */
    public void applyTimeout(HttpRequestBase httpRequest, String appName) {
        String serviceName = context.getEnvironment().getProperty(
                String.format("rpc.%s.name", appName).toLowerCase());
        String prefix = "spring.cloud.openfeign.client.config.";
        int connectTimeout = getFeignTimeout(prefix, serviceName, "connectTimeout", 1000);
        int readTimeout = getFeignTimeout(prefix, serviceName, "readTimeout", 15000);
        httpRequest.setConfig(RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(readTimeout)
                .setConnectionRequestTimeout(5000)
                .build());
    }

    private int getFeignTimeout(String prefix, String serviceName, String key, int defaultMs) {
        Integer value = null;
        if (serviceName != null) {
            value = context.getEnvironment().getProperty(prefix + serviceName + "." + key, Integer.class);
        }
        if (value == null) {
            value = context.getEnvironment().getProperty(prefix + "default." + key, Integer.class);
        }
        return value != null ? value : defaultMs;
    }

    /**
     * 执行后端请求，状态码+响应头+响应体全部流式透传到客户端
     * 适用于：JSON、文件下载、SSE流、二进制流
     */
    public void executeAndStream(HttpRequestBase httpRequest, HttpServletResponse response) throws IOException {
        try (CloseableHttpResponse backendResponse = client.execute(httpRequest)) {
            response.setStatus(backendResponse.getStatusLine().getStatusCode());
            for (Header header : backendResponse.getAllHeaders()) {
                if ("Transfer-Encoding".equalsIgnoreCase(header.getName())) continue;
                response.addHeader(header.getName(), header.getValue());
            }
            if (backendResponse.getEntity() != null) {
                try (InputStream bodyStream = backendResponse.getEntity().getContent()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bodyStream.read(buffer)) != -1) {
                        response.getOutputStream().write(buffer, 0, bytesRead);
                        response.getOutputStream().flush();
                    }
                }
            }
        }
    }
}
