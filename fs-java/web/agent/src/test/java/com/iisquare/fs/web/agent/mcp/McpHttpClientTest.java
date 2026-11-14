package com.iisquare.fs.web.agent.mcp;

import com.sun.net.httpserver.HttpServer;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class McpHttpClientTest {

    @Test
    public void sseEndpointDoesNotSwitchToStreamableHttp() throws Exception {
        AtomicInteger gets = new AtomicInteger();
        AtomicInteger posts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sse", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                gets.incrementAndGet();
                exchange.sendResponseHeaders(405, -1);
            } else {
                posts.incrementAndGet();
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/sse";
            try (McpHttpClient client = new McpHttpClient(
                    url, Collections.emptyMap(), 1000, 1000)) {
                client.initialize();
                Assert.fail("SSE 端点失败时应直接抛错");
            } catch (IllegalStateException expected) {
                Assert.assertTrue(expected.getMessage().contains("SSE 端点"));
            }
            Assert.assertEquals(1, gets.get());
            Assert.assertEquals(0, posts.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void streamableEndpointSendsPostWithoutProbeOrRetry() throws Exception {
        AtomicInteger gets = new AtomicInteger();
        AtomicInteger posts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                gets.incrementAndGet();
                exchange.sendResponseHeaders(405, -1);
            } else {
                posts.incrementAndGet();
                exchange.sendResponseHeaders(500, -1);
            }
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp";
            try (McpHttpClient client = new McpHttpClient(
                    url, Collections.emptyMap(), 1000, 1000)) {
                client.initialize();
                Assert.fail("流式 HTTP 首次请求失败时应直接抛错");
            } catch (IllegalStateException expected) {
                Assert.assertTrue(expected.getMessage().contains("HTTP 500"));
            }
            Assert.assertEquals(0, gets.get());
            Assert.assertEquals(1, posts.get());
        } finally {
            server.stop(0);
        }
    }
}
