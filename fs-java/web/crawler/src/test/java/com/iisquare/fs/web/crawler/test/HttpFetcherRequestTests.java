package com.iisquare.fs.web.crawler.test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.crawler.core.HttpFetcher;
import com.iisquare.fs.web.crawler.core.HttpDownloader;
import com.iisquare.fs.web.crawler.core.Worker;
import com.iisquare.fs.web.crawler.core.ZooJob;
import org.apache.http.client.methods.CloseableHttpResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class HttpFetcherRequestTests {

    private HttpServer server;
    private String url;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> query = new AtomicReference<>();

    @Before
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/echo", exchange -> {
            method.set(exchange.getRequestMethod());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] response = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(202, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/echo";
    }

    @After
    public void stop() {
        server.stop(0);
    }

    @Test
    public void supportsMethodsAndParameterTypes() throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("keyword", "hello world");
        for (String requestType : Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE")) {
            for (String parameterType : Arrays.asList("none", "form-data", "x-www-form-urlencoded", "json", "xml", "raw")) {
                method.set(null);
                body.set(null);
                contentType.set(null);
                query.set(null);
                HttpFetcher fetcher = new HttpFetcher();
                try {
                    fetcher.url(url).requestType(requestType).contentType(parameterType)
                            .payloadForm(form).body("{\"source\":\"test\"}")
                            .charset(StandardCharsets.UTF_8).downloader(new HttpDownloader() {
                                @Override
                                public String download(HttpFetcher fetcher, CloseableHttpResponse response) {
                                    throw new AssertionError("missing response content type should be read as text");
                                }
                            }).execute();
                    Assert.assertNull(fetcher.getLastException());
                    Assert.assertEquals(202, fetcher.getLastStatus());
                    Assert.assertEquals("{\"result\":\"ok\"}", fetcher.getLastResult());
                    Assert.assertEquals(requestType, method.get());
                    if ("GET".equals(requestType)) {
                        Assert.assertEquals("keyword=hello+world", query.get());
                        Assert.assertEquals("", body.get());
                        continue;
                    }
                    switch (parameterType) {
                        case "form-data":
                            Assert.assertTrue(body.get().contains("keyword"));
                            Assert.assertTrue(contentType.get().startsWith("multipart/form-data"));
                            break;
                        case "x-www-form-urlencoded":
                            Assert.assertEquals("keyword=hello+world", body.get());
                            Assert.assertTrue(contentType.get().startsWith("application/x-www-form-urlencoded"));
                            break;
                        case "json":
                            Assert.assertTrue(contentType.get().startsWith("application/json"));
                            break;
                        case "xml":
                            Assert.assertTrue(contentType.get().startsWith("application/xml"));
                            break;
                        case "raw":
                            Assert.assertTrue(contentType.get().startsWith("text/plain"));
                            break;
                        case "none":
                            Assert.assertEquals("", body.get());
                            Assert.assertNull(contentType.get());
                            break;
                    }
                } finally {
                    fetcher.close();
                }
            }
        }
    }

    @Test
    public void resolvesPlanPageRequestConfig() {
        ObjectNode template = DPUtil.objectNode();
        template.put("type", "plan");
        ObjectNode page = template.putObject("pages").putObject("search");
        page.put("requestType", "POST");
        page.put("contentType", "x-www-form-urlencoded");
        page.put("requestBody", "{\"keyword\":\"{keyword}\"}");
        page.putObject("payloadForm").put("keyword", "{keyword}");
        page.putObject("headers").put("Authorization", "Bearer {keyword}");
        ZooJob job = new ZooJob();
        job.template = template;
        ObjectNode task = DPUtil.objectNode();
        task.put("pageCode", "search");
        task.putObject("args").put("keyword", "测试");
        Assert.assertEquals("POST", Worker.requestType(job, task));
        Assert.assertEquals("x-www-form-urlencoded", Worker.contentType(job, task));
        Assert.assertEquals("{\"keyword\":\"测试\"}", Worker.requestBody(job, task));
        Assert.assertEquals("测试", Worker.payloadForm(job, task).get("keyword"));
        Assert.assertEquals("Bearer 测试", Worker.requestHeaders(job, task).get("Authorization"));
    }

    @Test
    public void keepsMultipartBoundaryAndHonorsCustomJsonType() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/vnd.api+json; charset=UTF-8");
        HttpFetcher json = new HttpFetcher();
        try {
            contentType.set(null);
            json.url(url).requestType("POST").contentType("json").body("{}")
                    .charset(StandardCharsets.UTF_8).headers(headers).execute();
            Assert.assertEquals("application/vnd.api+json; charset=UTF-8", contentType.get());
        } finally {
            json.close();
        }

        HttpFetcher multipart = new HttpFetcher();
        try {
            contentType.set(null);
            multipart.url(url).requestType("POST").contentType("form-data")
                    .payloadForm(new LinkedHashMap<>()).charset(StandardCharsets.UTF_8)
                    .headers(headers).execute();
            Assert.assertTrue(contentType.get().startsWith("multipart/form-data; boundary="));
        } finally {
            multipart.close();
        }
    }
}
