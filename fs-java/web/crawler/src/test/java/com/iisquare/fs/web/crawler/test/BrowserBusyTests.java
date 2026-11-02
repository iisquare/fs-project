package com.iisquare.fs.web.crawler.test;

import com.iisquare.fs.web.crawler.core.BrowserFetchResult;
import com.iisquare.fs.web.crawler.core.BrowserFetcher;
import com.iisquare.fs.web.crawler.core.BrowserNode;
import com.iisquare.fs.web.crawler.core.Worker;
import com.iisquare.fs.web.crawler.core.ZooKeeperClient;
import com.iisquare.fs.web.crawler.core.ZooJob;
import com.iisquare.fs.base.core.util.DPUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BrowserBusyTests {

    private HttpServer server;
    private String endpoint;
    private final AtomicInteger requests = new AtomicInteger();

    @Before
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/browser/fetch", exchange -> {
            exchange.getRequestBody().readAllBytes();
            String response;
            if (requests.getAndIncrement() == 0) {
                response = "{\"code\":503,\"message\":\"无空闲浏览器实例，请稍后重试\",\"data\":null}";
            } else {
                response = "{\"code\":0,\"message\":\"操作成功\",\"data\":{"
                        + "\"status\":200,\"html\":\"<html><body>ready</body></html>\","
                        + "\"location\":\"http://example.com\",\"coast\":12,\"exception\":\"\"}}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stop() {
        server.stop(0);
    }

    @Test
    public void returnsRetryableResultWhenBrowserResourcesAreBusy() {
        BrowserNode node = new BrowserNode();
        node.setId("127.0.0.1:" + server.getAddress().getPort() + ":browser");
        node.setHost("127.0.0.1");
        node.setPort(server.getAddress().getPort());
        node.setRole("browser");
        node.setMaxConcurrency(1);
        node.setActive(1);
        node.setQueued(0);
        node.setAvailable(0);
        node.setUpdatedTime(System.currentTimeMillis());

        ZooKeeperClient zookeeper = new ZooKeeperClient("127.0.0.1:2181/fs-spider", 2000, node.getId()) {
            @Override
            public Map<String, BrowserNode> browsers() {
                Map<String, BrowserNode> nodes = new LinkedHashMap<>();
                nodes.put(node.getId(), node);
                return nodes;
            }
        };
        BrowserFetcher fetcher = new BrowserFetcher(zookeeper);
        BrowserFetchResult result = fetcher.fetch(
                "http://example.com", "GET", "", "none", Map.of(), Map.of(),
                "UTF-8", 1000, "domcontentloaded", "");

        Assert.assertEquals(1, requests.get());
        Assert.assertEquals(0, result.getStatus());
        Assert.assertTrue(result.isRetryable());
        Assert.assertTrue(result.getException().contains("无空闲浏览器实例"));
    }

    @Test
    public void resolvesBrowserPolicyFromSiteAndPageWithoutTemplateFallback() {
        ZooJob job = new ZooJob();
        job.template = DPUtil.parseJSON("""
                {
                  "type": "broad",
                  "fetchMode": "browser",
                  "browserWaitSelector": ".legacy-template-selector",
                  "browserTimeout": 99999,
                  "sites": {
                    "example.com": {
                      "fetchMode": "browser",
                      "browserWaitUntil": "load",
                      "browserWaitSelector": ".site-selector",
                      "browserTimeout": 12000
                    },
                    "*": {
                      "fetchMode": "http",
                      "browserWaitUntil": "domcontentloaded",
                      "browserWaitSelector": ".fallback-selector",
                      "browserTimeout": 15000
                    }
                  }
                }
                """);
        com.fasterxml.jackson.databind.node.ObjectNode task = DPUtil.objectNode();
        task.put("url", "https://example.com/article");
        Assert.assertEquals("browser", Worker.fetchMode(job, task));
        Assert.assertEquals("load", Worker.browserWaitUntil(job, task));
        Assert.assertEquals(".site-selector", Worker.browserWaitSelector(job, task));
        Assert.assertEquals(12000, Worker.browserTimeout(job, task));

        task.put("url", "https://other.example.org/article");
        Assert.assertEquals("http", Worker.fetchMode(job, task));
        Assert.assertEquals("domcontentloaded", Worker.browserWaitUntil(job, task));
        Assert.assertEquals(".fallback-selector", Worker.browserWaitSelector(job, task));
        Assert.assertEquals(15000, Worker.browserTimeout(job, task));

        job.template = DPUtil.parseJSON("""
                {
                  "type": "plan",
                  "browserWaitSelector": ".legacy-template-selector",
                  "pages": {
                    "detail": {
                      "fetchMode": "browser",
                      "browserWaitUntil": "networkidle",
                      "browserWaitSelector": ".detail-selector",
                      "browserTimeout": 8000
                    }
                  }
                }
                """);
        com.fasterxml.jackson.databind.node.ObjectNode pageTask = DPUtil.objectNode();
        pageTask.put("url", "https://example.com/detail/1");
        pageTask.put("pageCode", "detail");
        Assert.assertEquals("browser", Worker.fetchMode(job, pageTask));
        Assert.assertEquals("networkidle", Worker.browserWaitUntil(job, pageTask));
        Assert.assertEquals(".detail-selector", Worker.browserWaitSelector(job, pageTask));
        Assert.assertEquals(8000, Worker.browserTimeout(job, pageTask));
    }
}
