package com.iisquare.fs.web.crawler.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class BrowserFetcher {

    private static final long NODE_MAX_AGE = 15000;
    private final ZooKeeperClient zookeeper;
    private final HttpClient client;
    private final AtomicLong turns = new AtomicLong();

    public BrowserFetcher(ZooKeeperClient zookeeper) {
        this.zookeeper = zookeeper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public BrowserFetchResult fetch(String url, String requestType, String requestBody,
                                    String contentType, Map<String, String> payloadForm,
                                    Map<String, String> headers, String charset, int timeout,
                                    String waitUntil, String waitSelector) {
        ObjectNode request = DPUtil.objectNode();
        request.put("url", url);
        request.put("requestType", requestType);
        request.put("requestBody", requestBody);
        request.put("contentType", contentType);
        request.put("charset", charset);
        request.put("timeout", timeout);
        request.put("waitUntil", waitUntil);
        request.put("waitSelector", waitSelector);
        request.put("scroll", false);
        request.set("payloadForm", DPUtil.toJSON(null == payloadForm ? Map.of() : payloadForm));
        request.set("headers", DPUtil.toJSON(null == headers ? Map.of() : headers));

        BrowserNode node = select();
        if (null == node) return failure(null, "没有可用的无头浏览器节点", true);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(
                            URI.create(node.endpoint() + "/browser/fetch"))
                    .timeout(Duration.ofMillis(timeout + 10000L))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            DPUtil.stringify(request), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode json = DPUtil.parseJSON(response.body());
            if (null == json) return failure(node, "浏览器节点返回内容无法解析", true);
            int code = json.at("/code").asInt(-1);
            if (code != 0) {
                String message = json.at("/message").asText("浏览器请求失败");
                return failure(node, message, code == 503 || response.statusCode() >= 500);
            }
            JsonNode storage = json.at("/data");
            BrowserFetchResult result = new BrowserFetchResult();
            result.setStatus(storage.at("/status").asInt());
            result.setResult(storage.at("/html").asText(""));
            result.setLocation(storage.at("/location").asText(url));
            result.setException(storage.at("/exception").asText(""));
            result.setWarning(storage.at("/warning").asText(""));
            result.setCoast(storage.at("/coast").asLong());
            result.setNodeId(node.getId());
            result.setRetryable(false);
            return result;
        } catch (Exception e) {
            return failure(node, e.getMessage(), true);
        }
    }

    private BrowserFetchResult failure(BrowserNode node, String message, boolean retryable) {
        BrowserFetchResult result = new BrowserFetchResult();
        result.setNodeId(null == node ? "" : node.getId());
        result.setStatus(0);
        result.setResult("");
        result.setLocation("");
        result.setException(DPUtil.empty(message) ? "浏览器节点请求失败" : message);
        result.setRetryable(retryable);
        return result;
    }

    private BrowserNode select() {
        List<BrowserNode> nodes = new ArrayList<>();
        for (BrowserNode node : zookeeper.browsers().values()) {
            if (null != node && node.fresh(NODE_MAX_AGE)) nodes.add(node);
        }
        if (nodes.isEmpty()) return null;
        nodes.sort(Comparator.comparingDouble(BrowserNode::load).thenComparing(BrowserNode::getId));
        double minimum = nodes.get(0).load();
        List<BrowserNode> balanced = new ArrayList<>();
        for (BrowserNode node : nodes) {
            if (node.load() - minimum > 0.05D) break;
            balanced.add(node);
        }
        int offset = (int) Math.floorMod(turns.getAndIncrement(), balanced.size());
        return balanced.get(offset);
    }
}
