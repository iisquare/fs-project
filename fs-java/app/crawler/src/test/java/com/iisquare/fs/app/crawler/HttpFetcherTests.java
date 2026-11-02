package com.iisquare.fs.app.crawler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.app.crawler.fetch.HttpFetcher;
import com.iisquare.fs.app.crawler.schedule.Template;
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

public class HttpFetcherTests {

    private HttpServer server;
    private String url;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestContentType = new AtomicReference<>();
    private final AtomicReference<String> requestQuery = new AtomicReference<>();

    @Before
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/echo", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] response = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, response.length);
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
    public void supportsCommonRequestTypes() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/vnd.api+json; charset=UTF-8");
        for (String method : Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE")) {
            requestMethod.set(null);
            requestBody.set(null);
            requestContentType.set(null);
            requestQuery.set(null);
            HttpFetcher fetcher = new HttpFetcher();
            try {
                fetcher.url(url).requestType(method).contentType("json").body("{\"method\":\"" + method + "\"}")
                        .charset("UTF-8").headers(headers).execute();
                Assert.assertNull(fetcher.getLastException());
                Assert.assertEquals(201, fetcher.getLastStatus());
                Assert.assertEquals(method, requestMethod.get());
                if ("GET".equals(method)) {
                    Assert.assertEquals("", requestBody.get());
                } else {
                    Assert.assertEquals("{\"method\":\"" + method + "\"}", requestBody.get());
                    Assert.assertEquals("application/vnd.api+json; charset=UTF-8", requestContentType.get());
                }
                Assert.assertEquals("{\"result\":\"ok\"}", fetcher.getLastResult());
            } finally {
                fetcher.close();
            }
        }
    }

    @Test
    public void supportsRequestParameterTypes() throws Exception {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("name", "hello world");
        for (String contentType : Arrays.asList("none", "form-data", "x-www-form-urlencoded", "json", "xml", "raw")) {
            requestBody.set(null);
            requestContentType.set(null);
            requestQuery.set(null);
            String body = "{\"type\":\"" + contentType + "\"}";
            HttpFetcher fetcher = new HttpFetcher();
            try {
                fetcher.url(url).requestType("POST").contentType(contentType)
                        .payloadForm(form).body(body).charset("UTF-8").execute();
                Assert.assertNull(fetcher.getLastException());
                Assert.assertEquals(201, fetcher.getLastStatus());
                switch (contentType) {
                    case "none":
                        Assert.assertEquals("", requestBody.get());
                        Assert.assertNull(requestContentType.get());
                        break;
                    case "form-data":
                        Assert.assertTrue(requestBody.get().contains("name=\"name\""));
                        Assert.assertTrue(requestBody.get().contains("hello world"));
                        Assert.assertTrue(requestContentType.get().startsWith("multipart/form-data"));
                        break;
                    case "x-www-form-urlencoded":
                        Assert.assertEquals("name=hello+world", requestBody.get());
                        Assert.assertTrue(requestContentType.get().startsWith("application/x-www-form-urlencoded"));
                        break;
                    case "json":
                        Assert.assertEquals(body, requestBody.get());
                        Assert.assertTrue(requestContentType.get().startsWith("application/json"));
                        break;
                    case "xml":
                        Assert.assertEquals(body, requestBody.get());
                        Assert.assertTrue(requestContentType.get().startsWith("application/xml"));
                        break;
                    case "raw":
                        Assert.assertEquals(body, requestBody.get());
                        Assert.assertTrue(requestContentType.get().startsWith("text/plain"));
                        break;
                }
            } finally {
                fetcher.close();
            }
        }

        HttpFetcher fetcher = new HttpFetcher();
        try {
            fetch(fetcher, url + "?fixed=1#section", "GET", "none", form);
            Assert.assertEquals("fixed=1&name=hello+world", requestQuery.get());
            Assert.assertEquals("", requestBody.get());
        } finally {
            fetcher.close();
        }
    }

    @Test
    public void loadsPublishedPageRequestConfig() {
        ObjectNode config = DPUtil.objectNode();
        config.put("requestType", "post");
        config.put("contentType", "form-data");
        config.put("requestBody", "{\"source\":\"{keyword}\"}");
        ObjectNode form = config.putObject("payloadForm");
        form.put("keyword", "{keyword}");
        Template template = new Template(config);
        Assert.assertEquals("POST", template.getRequestType());
        Assert.assertEquals("form-data", template.getContentType());
        Assert.assertEquals("{\"source\":\"{keyword}\"}", template.getRequestBody());
        Assert.assertEquals("{keyword}", template.getPayloadForm().get("keyword"));
    }

    private void fetch(HttpFetcher fetcher, String targetUrl, String method, String contentType, Map<String, String> form) {
        fetcher.url(targetUrl).requestType(method).contentType(contentType)
                .payloadForm(form).charset("UTF-8").execute();
    }
}
