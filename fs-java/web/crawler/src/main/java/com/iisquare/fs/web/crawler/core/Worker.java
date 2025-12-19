package com.iisquare.fs.web.crawler.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.crawler.service.NodeService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private Map<String, Object> process(HttpFetcher fetcher) {
        ObjectNode storage = data.putObject("storage");
        String url = data.at("/task/url").asText();
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
        for (Header header : fetcher.getLastResponseHeaders()) {
            if (header.getName().equals(HttpHeaders.CONTENT_TYPE)) {
                storage.put("media", header.getValue());
                break;
            }
        }
        Exception exception = fetcher.getLastException();
        if (null != exception) {
            storage.put("exception", exception.getMessage());
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
