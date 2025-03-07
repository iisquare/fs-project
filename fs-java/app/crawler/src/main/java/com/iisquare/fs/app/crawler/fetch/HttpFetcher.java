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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
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
        this.clear();
        HttpGet hg;
        try {
            hg = new HttpGet(url);
        } catch (Exception e) {
            this.lastException = e;
            return this;
        }
        hg.setConfig(null == config ? defaultConfig : config);
        if (null != this.headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                hg.setHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = client.execute(hg);
        } catch (IOException e) {
            this.lastException = e;
            return this;
        }
        this.lastStatus = response.getStatusLine().getStatusCode();
        this.lastRequestHeaders = hg.getAllHeaders();
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
        try {
            response.close();
        } catch (IOException e) {}
        return this;
    }

    @Override
    public void close() throws IOException {
        if (null != client) client.close();
    }
}
