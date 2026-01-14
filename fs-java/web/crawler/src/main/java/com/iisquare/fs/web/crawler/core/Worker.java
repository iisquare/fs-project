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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Slf4j
public class Worker implements Runnable {

    private Thread thread;
    /**
     * 参数格式：{
     *     time: 任务拉取时间,
     *     nodeId: '认证节点标识',
     *     token: {},
     *     task: {},
     * }
     */
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
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("Pragma", "no-cache");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
        return headers;
    }

    public static String bucket(ZooJob job, JsonNode task, URI uri){
        if (null == job) return "fs-spider";
        String bucket = null;
        switch (job.template.at("/type").asText()) {
            case "broad":
            case "single": {
                JsonNode sites = job.template.at("/sites");
                String domain = uri.getHost();
                bucket = sites.at("/" + domain + "/bucket").asText();
                if (DPUtil.empty(bucket)) {
                    domain = "*";
                    bucket = sites.at("/" + domain + "/bucket").asText();
                }
                break;
            }
            case "plan": {
                String pageCode = task.at("/pageCode").asText();
                bucket = job.template.at("/pages/" + pageCode + "/bucket").asText();
                break;
            }
        }
        return DPUtil.empty(bucket) ? "fs-spider" : bucket;
    }

    public static Charset charset(ZooJob job, JsonNode task, URI uri) {
        if (null == job) return HttpFetcher.DEFAULT_CHARSET;
        String charset = null;
        switch (job.template.at("/type").asText()) {
            case "broad":
            case "single": {
                JsonNode sites = job.template.at("/sites");
                String domain = uri.getHost();
                charset = sites.at("/" + domain + "/charset").asText();
                if (DPUtil.empty(charset)) {
                    domain = "*";
                    charset = sites.at("/" + domain + "/charset").asText();
                }
                break;
            }
            case "plan": {
                String pageCode = task.at("/pageCode").asText();
                charset = job.template.at("/pages/" + pageCode + "/charset").asText();
                break;
            }
        }
        if (DPUtil.empty(charset)) return HttpFetcher.DEFAULT_CHARSET;
        try {
            return Charset.forName(charset);
        } catch (UnsupportedCharsetException e) {
            return HttpFetcher.DEFAULT_CHARSET;
        }
    }

    public static int connectTimeout(ZooJob job, JsonNode task, URI uri){
        if (null == job) return 0;
        int timeout = 0;
        switch (job.template.at("/type").asText()) {
            case "broad":
            case "single": {
                JsonNode sites = job.template.at("/sites");
                String domain = uri.getHost();
                timeout = sites.at("/" + domain + "/connectTimeout").asInt(0);
                if (0 == timeout) {
                    domain = "*";
                    timeout = sites.at("/" + domain + "/connectTimeout").asInt(0);
                }
                break;
            }
            case "plan": {
                String pageCode = task.at("/pageCode").asText();
                timeout = job.template.at("/pages/" + pageCode + "/connectTimeout").asInt();
                break;
            }
        }
        return timeout;
    }

    public static int socketTimeout(ZooJob job, JsonNode task, URI uri){
        if (null == job) return 0;
        int timeout = 0;
        switch (job.template.at("/type").asText()) {
            case "broad":
            case "single": {
                JsonNode sites = job.template.at("/sites");
                String domain = uri.getHost();
                timeout = sites.at("/" + domain + "/socketTimeout").asInt(0);
                if (0 == timeout) {
                    domain = "*";
                    timeout = sites.at("/" + domain + "/socketTimeout").asInt(0);
                }
                break;
            }
            case "plan": {
                String pageCode = task.at("/pageCode").asText();
                timeout = job.template.at("/pages/" + pageCode + "/socketTimeout").asInt();
                break;
            }
        }
        return timeout;
    }

    private Map<String, Object> process(HttpFetcher fetcher) {
        JsonNode task = data.at("/task");
        ZooJob job = nodeService.zookeeper().job(task.at("/jobId").asText());
        if (null == job) {
            return ApiUtil.result(1401, "load job failed", data);
        }
        ObjectNode storage = data.putObject("storage");
        String url = task.at("/url").asText();
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            storage.put("exception", e.getMessage());
            return ApiUtil.result(0, "create url failed:" + e.getMessage(), data);
        }
        int socketTimeout = socketTimeout(job, task, uri);
        int connectTimeout = connectTimeout(job, task, uri);
        if (connectTimeout > 0 && socketTimeout > 0) {
            RequestConfig.Builder builder = RequestConfig.custom()
                    .setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout);
            fetcher.config(builder.build());
        }
        fetcher.charset(charset(job, task, uri)).downloader(new HttpDownloader() {
            @Override
            public String download(HttpFetcher fetcher, CloseableHttpResponse response) throws Exception {
                String bucket = bucket(job, task, uri);
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
        String referer = task.at("/referer").asText();
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
            storage.put("exception", ApiUtil.getStackTrace(exception));
        }
        storage.put("html", html);
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
