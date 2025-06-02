package com.iisquare.fs.base.web.sse;

import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SsePlainRequestPool implements Closeable {

    private CloseableHttpClient client;
    private RequestConfig config;

    public SsePlainRequestPool() {
        SSLConnectionSocketFactory scsf;
        try {
            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(20000); // 最大连接数
        pooling.setDefaultMaxPerRoute(1000); // 默认的每个路由的最大连接数

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setSSLSocketFactory(scsf);
        builder.setConnectionManager(pooling);
        init(builder);
    }

    public SsePlainRequestPool(HttpClientBuilder builder) {
        init(builder);
    }

    private void init(HttpClientBuilder builder) {
        client = builder.build();
        config = RequestConfig.custom()
                .setSocketTimeout(600000)
                .setConnectTimeout(3000)
                .setConnectionRequestTimeout(5000)
                .build();
    }

    public RequestConfig config() {
        return config;
    }


    @Override
    public void close() throws IOException {
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

    public CloseableHttpResponse execute(SsePlainRequest request) throws Exception {
        HttpRequestBase http = request.request(true);
        return this.client.execute(http);
    }

    public boolean isStream(CloseableHttpResponse response) {
        String type = MediaType.TEXT_EVENT_STREAM.toString();
        Header[] headers = response.getHeaders(HttpHeaders.CONTENT_TYPE);
        for (Header header : headers) {
            if (header.getValue().startsWith(type)) {
                return true;
            }
        }
        return false;
    }

    public SseEmitter process(SsePlainRequest request, SsePlainEmitter emitter) {
        emitter.onError((e) -> {
            request.abort(); // 中断模型端处理请求
        }).onTimeout(() -> {
            request.abort(); // 中断模型端处理请求
        });
        CloseableHttpResponse response;
        try {
            response = execute(request);
        } catch (Exception e) {
            request.onError(null, e, false); // 转交给SsePlainRequest进行异常处理
            FileUtil.close(request);
            return emitter.sync();
        }
        emitter.setMediaType(response); // 需要在异步返回前，确定请求响应类型
        return emitter.async(() -> process(request, response));
    }

    public boolean process(SsePlainRequest request, CloseableHttpResponse response) {
        boolean isStream = isStream(response);
        InputStream stream = null;
        InputStreamReader reader = null;
        BufferedReader buffer = null;
        try {
            stream = response.getEntity().getContent();
            reader = new InputStreamReader(stream);
            buffer = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;
            while (!request.isAborted() && (line = buffer.readLine()) != null) {
                if (isStream) {
                    boolean result = request.onLine(response, line, true);
                    if (!result) {
                        request.abort(); // 终止请求，断开与后端服务连接
                        break;
                    }
                } else {
                    sb.append(line).append("\n");
                }
            }
            if (!isStream) {
                request.onLine(response, sb.toString(), false);
            }
            return true;
        } catch (Exception e) { // 读取响应异常，可能是服务端断开连接
            request.failure = e;
            request.onError(response, e, isStream);
            return false;
        } finally {
            FileUtil.close(buffer, reader, stream, response, request);
        }
    }

    public boolean submit(SsePlainRequest request) throws Exception {
        return process(request, execute(request));
    }

    public HttpRequestBase applyHeaders(HttpRequestBase http, HttpServletRequest request, List<String> headers) {
        for (String header : headers) {
            http.addHeader(header, request.getHeader(header));
        }
        return http;
    }

}
