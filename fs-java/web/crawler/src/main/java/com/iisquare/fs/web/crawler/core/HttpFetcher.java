package com.iisquare.fs.web.crawler.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
public class HttpFetcher implements Closeable {

    private static final String[][] auto = new String[][]{
        new String[]{"^", "|"},
        new String[]{"%5E", "%7C"}
    };
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final int DEFAULT_STATUS = 0;
    private final CloseableHttpClient client;
    private String url;
    private Map<String, String> headers;
    private int lastStatus = DEFAULT_STATUS;
    private String lastResult;
    private String lastLocation;
    private Charset charset = DEFAULT_CHARSET;
    private Exception lastException;
    private Header[] lastRequestHeaders;
    private Header[] lastResponseHeaders;
    private RequestConfig config;
    private final RequestConfig defaultConfig;

    public HttpFetcher() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true)
                .build();
        client = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                .setRedirectStrategy(redirectStrategy()).build();
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
                lastLocation = autoUrl(location);
                return super.createLocationURI(lastLocation);
            }
        };
    }

    public void reset() {
        this.clear();
        this.url = null;
        this.headers = null;
        this.charset = DEFAULT_CHARSET;
        this.config = null;
    }

    public HttpFetcher clear() {
        this.lastResult = "";
        this.lastLocation = "";
        this.lastStatus = DEFAULT_STATUS;
        this.lastException = null;
        this.lastRequestHeaders = new Header[]{};
        this.lastResponseHeaders = new Header[]{};
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

    public HttpFetcher headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HttpFetcher charset(Charset charset) {
        if (DPUtil.empty(charset)) charset = DEFAULT_CHARSET;
        this.charset = charset;
        return this;
    }

    public HttpFetcher execute(HttpRequestBase request) {
        this.clear();
        request.setConfig(null == config ? defaultConfig : config);
        if (null != this.headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.setHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        CloseableHttpResponse response;
        try {
            response = client.execute(request);
        } catch (IOException e) {
            this.lastException = e;
            return this;
        }
        this.lastStatus = response.getStatusLine().getStatusCode();
        this.lastRequestHeaders = request.getAllHeaders();
        this.lastResponseHeaders = response.getAllHeaders();
        if (200 == this.lastStatus) {
            HttpEntity entity = response.getEntity();
            try {
                this.lastResult = EntityUtils.toString(entity, charset);
            } catch (IOException e) {
                this.lastException = e;
                return this;
            }
        }
        FileUtil.close(response);
        return this;
    }

    public HttpFetcher post(JsonNode body) {
        new HttpPost(url);
        HttpPost request;
        try {
            request = new HttpPost(url);
        } catch (Exception e) {
            this.lastException = e;
            return this;
        }
        StringEntity entity = new StringEntity(body.toString(), charset);
        entity.setContentType("application/json");
        request.setEntity(entity);
        return this.execute(request);
    }

    public HttpFetcher get() {
        HttpGet request;
        try {
            request = new HttpGet(url);
        } catch (Exception e) {
            this.lastException = e;
            return this;
        }
        return this.execute(request);
    }

    @Override
    public void close() throws IOException {
        if (null != client) client.close();
    }
}
