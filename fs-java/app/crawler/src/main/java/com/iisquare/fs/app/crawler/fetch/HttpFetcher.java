package com.iisquare.fs.app.crawler.fetch;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

@Getter
public class HttpFetcher implements Closeable {

    private static final String[][] auto = new String[][]{
        new String[]{"^", "|"},
        new String[]{"%5E", "%7C"}
    };
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final int DEFAULT_STATUS = 0;
    private CloseableHttpClient client;
    private String url;
    private String requestType = "GET";
    private String requestBody;
    private String contentType = "none";
    private Map<String, String> payloadForm;
    private Map<String, String> headers;
    private int lastStatus = DEFAULT_STATUS;
    private String lastResult;
    private String charset = DEFAULT_CHARSET;
    private Exception lastException;
    private Header[] lastRequestHeaders;
    private Header[] lastResponseHeaders;
    private RequestConfig config;
    private RequestConfig defaultConfig;

    public HttpFetcher() {
        client = HttpClients.custom().setRedirectStrategy(redirectStrategy()).build();
        RequestConfig.Builder builder = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(60000);
        defaultConfig = builder.build();
    }

    public String autoUrl(String url) {
        return StringUtils.replaceEachRepeatedly(url, auto[0], auto[1]);
    }

    public RedirectStrategy redirectStrategy() {
        return new DefaultRedirectStrategy(){
            @Override
            protected URI createLocationURI(String location) throws ProtocolException {
                return super.createLocationURI(autoUrl(location));
            }
        };
    }

    public void reset() {
        this.clear();
        this.url = null;
        this.requestType = "GET";
        this.requestBody = null;
        this.contentType = "none";
        this.payloadForm = null;
        this.headers = null;
        this.charset = DEFAULT_CHARSET;
        this.config = null;
    }

    public HttpFetcher clear() {
        this.lastResult = null;
        this.lastStatus = DEFAULT_STATUS;
        this.lastException = null;
        this.lastRequestHeaders = null;
        this.lastResponseHeaders = null;
        return this;
    }

    public HttpFetcher config(RequestConfig config) {
        this.config = config;
        return this;
    }

    public HttpFetcher url(String url) {
        this.url = autoUrl(url);
        return this;
    }

    public HttpFetcher requestType(String requestType) {
        String value = DPUtil.trim(requestType).toUpperCase(Locale.ROOT);
        this.requestType = value.isEmpty() ? "GET" : value;
        return this;
    }

    public HttpFetcher body(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public HttpFetcher contentType(String contentType) {
        String value = DPUtil.trim(contentType).toLowerCase(Locale.ROOT);
        this.contentType = value.isEmpty() ? "none" : value;
        return this;
    }

    public HttpFetcher payloadForm(Map<String, String> payloadForm) {
        this.payloadForm = payloadForm;
        return this;
    }

    public HttpFetcher headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HttpFetcher charset(String charset) {
        if (DPUtil.empty(charset)) charset = DEFAULT_CHARSET;
        this.charset = charset;
        return this;
    }

    public HttpFetcher get() {
        return execute();
    }

    public HttpFetcher execute() {
        this.clear();
        HttpRequestBase request;
        try {
            String targetUrl = "GET".equals(requestType) ? buildUrl(url, payloadForm) : url;
            request = buildRequest(targetUrl);
        } catch (Exception e) {
            this.lastException = e;
            return this;
        }
        request.setConfig(null == config ? defaultConfig : config);
        try {
            HttpEntity entity = buildEntity();
            if (null != entity && request instanceof HttpEntityEnclosingRequestBase) {
                ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
            }
        } catch (Exception e) {
            this.lastException = e;
            return this;
        }
        if (null != this.headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("form-data".equals(contentType) && "Content-Type".equalsIgnoreCase(entry.getKey())) continue;
                request.setHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = client.execute(request);
            this.lastStatus = response.getStatusLine().getStatusCode();
            this.lastRequestHeaders = request.getAllHeaders();
            this.lastResponseHeaders = response.getAllHeaders();
            if (lastStatus >= 200 && lastStatus < 300) {
                HttpEntity entity = response.getEntity();
                if (null != entity) {
                    this.lastResult = EntityUtils.toString(entity, charset);
                }
            }
        } catch (IOException e) {
            this.lastException = e;
            return this;
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {}
            }
        }
        return this;
    }

    private HttpRequestBase buildRequest(String targetUrl) {
        switch (requestType) {
            case "GET":
                return new HttpGet(targetUrl);
            case "POST":
                return new HttpPost(targetUrl);
            case "PUT":
                return new HttpPut(targetUrl);
            case "PATCH":
                return new HttpPatch(targetUrl);
            case "DELETE":
                return new HttpDeleteWithBody(targetUrl);
            default:
                throw new IllegalArgumentException("unsupported request type: " + requestType);
        }
    }

    private HttpEntity buildEntity() throws Exception {
        switch (contentType) {
            case "form-data": {
                return multipartEntity();
            }
            case "x-www-form-urlencoded": {
                StringEntity entity = new StringEntity(buildQueryString(payloadForm), charset);
                entity.setContentType("application/x-www-form-urlencoded; charset=" + charset);
                return entity;
            }
            case "json": {
                StringEntity entity = new StringEntity(null == requestBody ? "" : requestBody, charset);
                entity.setContentType("application/json; charset=" + charset);
                return entity;
            }
            case "xml": {
                StringEntity entity = new StringEntity(null == requestBody ? "" : requestBody, charset);
                entity.setContentType("application/xml; charset=" + charset);
                return entity;
            }
            case "raw": {
                StringEntity entity = new StringEntity(null == requestBody ? "" : requestBody, charset);
                entity.setContentType("text/plain; charset=" + charset);
                return entity;
            }
            default:
                return null;
        }
    }

    private HttpEntity multipartEntity() throws Exception {
        String boundary = "----fs-spider-" + System.currentTimeMillis();
        StringBuilder body = new StringBuilder();
        if (null != payloadForm) {
            for (Map.Entry<String, String> entry : payloadForm.entrySet()) {
                body.append("--").append(boundary).append("\r\n");
                body.append("Content-Disposition: form-data; name=\"")
                        .append(entry.getKey()).append("\"\r\n\r\n");
                body.append(null == entry.getValue() ? "" : entry.getValue()).append("\r\n");
            }
        }
        body.append("--").append(boundary).append("--\r\n");
        StringEntity entity = new StringEntity(body.toString(), charset);
        entity.setContentType("multipart/form-data; boundary=" + boundary);
        return entity;
    }

    private String buildUrl(String baseUrl, Map<String, String> form) throws Exception {
        String query = buildQueryString(form);
        if (DPUtil.empty(query)) return baseUrl;
        int anchor = baseUrl.indexOf('#');
        String fragment = anchor < 0 ? "" : baseUrl.substring(anchor);
        String target = anchor < 0 ? baseUrl : baseUrl.substring(0, anchor);
        String separator = !target.contains("?") ? "?" : (target.endsWith("?") || target.endsWith("&") ? "" : "&");
        return target + separator + query + fragment;
    }

    private String buildQueryString(Map<String, String> form) throws Exception {
        StringBuilder result = new StringBuilder();
        if (null != form) {
            for (Map.Entry<String, String> entry : form.entrySet()) {
                if (result.length() > 0) result.append('&');
                result.append(URLEncoder.encode(entry.getKey(), charset)).append('=');
                if (null != entry.getValue()) {
                    result.append(URLEncoder.encode(entry.getValue(), charset));
                }
            }
        }
        return result.toString();
    }

    private static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        private HttpDeleteWithBody(String url) {
            setURI(URI.create(url));
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }

    @Override
    public void close() throws IOException {
        if (null != client) client.close();
    }
}
