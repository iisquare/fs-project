package com.iisquare.fs.web.crawler.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.jsoup.util.JsoupUtil;
import com.iisquare.fs.web.crawler.util.HttpClientUtil;
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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final int DEFAULT_STATUS = 0;
    private final CloseableHttpClient client;
    private String url;
    private HttpDownloader downloader;
    private Map<String, String> headers;
    private int lastStatus = DEFAULT_STATUS;
    private String lastResult;
    private String lastLocation;
    private Charset charset = DEFAULT_CHARSET;
    private Exception lastException;
    private Header[] lastRequestHeaders;
    private Header[] lastResponseHeaders;
    private String lastResponseContentType;
    private RequestConfig config;
    private final RequestConfig defaultConfig;

    public HttpFetcher() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true)
                .build();
        client = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                .setRedirectStrategy(redirectStrategy()).build();
        RequestConfig.Builder builder = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(15000);
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
        this.downloader = null;
    }

    public HttpFetcher clear() {
        this.lastResult = "";
        this.lastLocation = "";
        this.lastStatus = DEFAULT_STATUS;
        this.lastException = null;
        this.lastRequestHeaders = new Header[]{};
        this.lastResponseHeaders = new Header[]{};
        this.lastResponseContentType = "";
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

    public HttpFetcher downloader(HttpDownloader downloader) {
        this.downloader = downloader;
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
        } catch (Exception e) {
            this.lastException = e;
            return this;
        }
        this.lastStatus = response.getStatusLine().getStatusCode();
        this.lastRequestHeaders = request.getAllHeaders();
        this.lastResponseHeaders = response.getAllHeaders();
        if (200 == this.lastStatus) {
            HttpEntity entity = response.getEntity();
            entity = HttpClientUtil.decode(entity);
            Header contentType = entity.getContentType();
            if (null != contentType) {
                this.lastResponseContentType = contentType.getValue();
            }
            if (null == this.downloader || isPlainContent(this.lastResponseContentType)) {
                try {
                    ContentType type = ContentType.get(entity);
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    if (null != type.getCharset()) { // 以响应头中的类型为准
                        this.lastResult = new String(bytes, type.getCharset());
                    } else {
                        this.lastResult = new String(bytes, this.charset); // 尝试采用指定编码进行解析
                        Document document = Jsoup.parse(this.lastResult, this.url);
                        String jcs = JsoupUtil.charset(document);
                        if (!DPUtil.empty(jcs)) {
                            try {
                                Charset cs = Charset.forName(jcs);
                                if (!this.charset.equals(cs)) { // 页面编码与指定编码不一致
                                    this.lastResult = new String(bytes, cs); // 采用页面编码重新解析
                                }
                            } catch (Exception ignored) {} // 忽略此处编码异常
                        }
                    }
                } catch (Exception e) {
                    this.lastException = e;
                }
            } else {
                try {
                    this.lastResult = this.downloader.download(this, response);
                } catch (Exception e) {
                    this.lastException = e;
                }
            }
        }
        FileUtil.close(response);
        return this;
    }

    public boolean isPlainContent(String responseContentType) {
        if (responseContentType.contains("text")) return true;
        if (responseContentType.contains("json")) return true;
        if (responseContentType.contains("html")) return true;
        if (responseContentType.contains("xml")) return true;
        return false;
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
