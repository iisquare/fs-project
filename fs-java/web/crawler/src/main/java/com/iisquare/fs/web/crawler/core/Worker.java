package com.iisquare.fs.web.crawler.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.crawler.service.NodeService;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Slf4j
public class Worker implements Runnable {

    private Thread thread;
    private ObjectNode data;
    private NodeService nodeService;

    public Worker() {}

    public void call(NodeService nodeService, ObjectNode data) {
        this.nodeService = nodeService;
        this.data = data;
        this.thread.start();
    }

    public static Map<String, String> defaultHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("Pragma", "no-cache");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
        return headers;
    }

    public String bucket(ZooJob job, URI uri){
        if (null == job) return "fs-spider";
        JsonNode sites = job.template.at("/sites");
        String domain = uri.getHost();
        if (!sites.has(domain)) domain = "*";
        String collection = sites.at("/" + domain + "/bucket").asText();
        return DPUtil.empty(collection) ? "fs-spider" : collection;
    }

    public Charset charset(ZooJob job, URI uri) {
        if (null == job) return HttpFetcher.DEFAULT_CHARSET;
        JsonNode sites = job.template.at("/sites");
        String domain = uri.getHost();
        if (!sites.has(domain)) domain = "*";
        String charset = sites.at("/" + domain + "/charset").asText();
        if (DPUtil.empty(charset)) return HttpFetcher.DEFAULT_CHARSET;
        try {
            return Charset.forName(charset);
        } catch (UnsupportedCharsetException e) {
            return HttpFetcher.DEFAULT_CHARSET;
        }
    }

    public int connectTimeout(ZooJob job, URI uri){
        if (null == job) return 0;
        JsonNode sites = job.template.at("/sites");
        String domain = uri.getHost();
        if (!sites.has(domain)) domain = "*";
        return sites.at("/" + domain + "/connectTimeout").asInt(0);
    }

    public int socketTimeout(ZooJob job, URI uri){
        if (null == job) return 0;
        JsonNode sites = job.template.at("/sites");
        String domain = uri.getHost();
        if (!sites.has(domain)) domain = "*";
        return sites.at("/" + domain + "/socketTimeout").asInt(0);
    }

    public int retryCount(ZooJob job, URI uri){
        if (null == job) return 0;
        JsonNode sites = job.template.at("/sites");
        String domain = uri.getHost();
        if (!sites.has(domain)) domain = "*";
        return sites.at("/" + domain + "/retryCount").asInt(0);
    }

    private Map<String, Object> process(HttpFetcher fetcher) {
        ZooJob job = nodeService.zookeeper().job(data.at("/task/jobId").asText());
        if (null == job) {
            return ApiUtil.result(1401, "load job failed", data);
        }
        ObjectNode storage = data.putObject("storage");
        String url = data.at("/task/url").asText();
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            storage.put("exception", e.getMessage());
            return ApiUtil.result(0, "create url failed:" + e.getMessage(), data);
        }
        int socketTimeout = socketTimeout(job, uri);
        int connectTimeout = connectTimeout(job, uri);
        if (connectTimeout > 0 && socketTimeout > 0) {
            RequestConfig.Builder builder = RequestConfig.custom()
                    .setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout);
            fetcher.config(builder.build());
        }
        fetcher.charset(charset(job, uri)).downloader(new HttpDownloader() {
            @Override
            public String download(HttpFetcher fetcher, CloseableHttpResponse response) throws Exception {
                String bucket = bucket(job, uri);
                String object = String.format("/%s/%s", DPUtil.dateTime("yyyy-MM-dd"), UUID.randomUUID());
                PutObjectArgs.Builder builder = PutObjectArgs.builder();
                builder.bucket(bucket);
                builder.object(object);
                if (!DPUtil.empty(fetcher.getLastResponseContentType())) {
                    builder.contentType(fetcher.getLastResponseContentType());
                }
                builder.stream(response.getEntity().getContent(), response.getEntity().getContentLength(), -1);
                ObjectWriteResponse result = nodeService.minio().putObject(builder.build());
                ObjectNode data = DPUtil.objectNode();
                data.put("bucket", bucket);
                data.put("object", object);
                if (null != result) {
                    data.put("bucket", result.bucket());
                    data.put("object", result.object());
                    data.put("region", result.region());
                    data.put("etag", result.etag());
                }
                return DPUtil.stringify(data);
            }
        });
        String referer = data.at("/task/referer").asText();
        Map<String, String> headers = defaultHeaders();
        if (!DPUtil.empty(referer)) {
            headers.put("Referer", referer);
        }
        long time = System.currentTimeMillis();
        fetcher.url(url).headers(headers).get();
        storage.put("coast", System.currentTimeMillis() - time);
        int status = fetcher.getLastStatus();
        String html = fetcher.getLastResult();
        storage.put("status", status);
        storage.put("location", fetcher.getLastLocation());
        storage.put("media", fetcher.getLastResponseContentType());
        Exception exception = fetcher.getLastException();
        if (null != exception) {
            storage.put("exception", exception.getMessage());
        }
        storage.put("html", html);
        int retryCount = data.at("/task/retryCount").asInt();
        if (!Arrays.asList(200, 404).contains(fetcher.getLastStatus()) && retryCount < retryCount(job, uri)) {
            return ApiUtil.result(1402, "fetcher task failed, retry " + retryCount + " times", data);
        }
        return ApiUtil.result(0, null, data);
    }

    @Override
    public void run() {
        HttpFetcher fetcher = null;
        do {
            try {
                fetcher = nodeService.fetcherPool().borrowObject();
            } catch (Exception e) {
                log.warn("worker borrow fetcher failed", e);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
            }
        } while (fetcher == null);
        do {
            Map<String, Object> result;
            try {
                result = process(fetcher);
            } catch (Exception e) {
                log.error("worker process fetcher failed", e);
                result = ApiUtil.result(7500, e.getMessage(), data);
            }
            if (!nodeService.report(fetcher, result)) break;
            JsonNode json = DPUtil.parseJSON(fetcher.getLastResult());
            if (null == json || 0 != json.at("/code").asInt(-1)) { // 上报结果异常
                log.warn("worker report failed: {}", json, fetcher.getLastException());
            }
        } while ((data = nodeService.checkSynchronousQueue()) != null);
        try {
            nodeService.fetcherPool().returnObject(fetcher);
        } catch (Exception e) {
            log.warn("worker return fetcher failed", e);
        }
        try {
            nodeService.workerPool().returnObject(this);
        } catch (Exception e) {
            log.warn("worker return self failed", e);
        }
    }

}
