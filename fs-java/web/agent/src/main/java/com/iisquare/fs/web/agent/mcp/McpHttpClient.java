package com.iisquare.fs.web.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP 客户端（HTTP 传输）：工具方法同步（initialize / tools/list）与调用（tools/call）都走它。
 * 两种传输按地址自动判别：
 *
 * - HTTP + SSE（2024-11-05）：GET 建立事件流，服务端在流里下发消息端点，之后 POST 消息、结果回到流里。
 *   自家 fs-mcp-server 的 {@code /sse} 属于这种。
 * - 流式 HTTP（2025-03-26 起，标准路径 {@code /mcp}）：只 POST，结果就在本次响应体里（JSON 或 SSE 帧），
 *   会话标识放在 {@code Mcp-Session-Id} 响应头。高德 {@code https://mcp.amap.com/mcp} 属于这种，
 *   它对 GET 直接返回 405，只做 SSE 的客户端连不上。
 *
 * 为什么不直接用 MCP Java SDK：SDK 的 SSE 传输用 {@code baseUri.resolve(endpoint)} 解析服务端下发的消息
 * 端点，服务地址带反向代理前缀时会把前缀丢掉（{@code http://host/api/agent/sse} → POST 落到
 * {@code http://host/mcp/message}），而且它访问不了只提供流式 HTTP 的服务。这里自己实现握手，
 * 端点解析「按 SSE 地址的父路径补全优先、标准解析兜底」，并补齐规范要求的请求头
 * （MCP-Protocol-Version、Mcp-Session-Id）以及 POST 响应内联结果的解析。
 *
 * 只实现同步与调用需要的请求：initialize、notifications/initialized、tools/list、tools/call。
 */
public class McpHttpClient implements AutoCloseable {

    /** SSE 传输的协议版本：与原 SDK 保持一致，行为不变 */
    private static final String SSE_PROTOCOL_VERSION = "2024-11-05";
    /** 流式 HTTP 传输的协议版本：该传输自 2025-03-26 引入 */
    private static final String STREAMABLE_PROTOCOL_VERSION = "2025-06-18";
    /**
     * 协议版本请求头：规范要求 HTTP 传输在初始化之后每个请求都带。原 SDK 每个请求都会带；
     * 缺了它，规范严格的 MCP 服务会直接拒掉 tools/list、tools/call（表现为「以前能同步，现在不行」）
     */
    private static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    /** 流式 HTTP 的会话标识：服务端在 initialize 响应头里下发，后续请求都要回传 */
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    /** 流式 HTTP 的 POST 必须同时声明两种可接受响应类型，否则规范实现会返回 406 */
    private static final String STREAMABLE_ACCEPT = "application/json, text/event-stream";
    /** 清单分页上限：服务端游标异常时不至于无限翻页 */
    private static final int MAX_PAGES = 50;
    /** 等待 SSE 消息端点的上限：超过它还没等到，就按流式 HTTP 再试一次 */
    private static final long ENDPOINT_TIMEOUT = 8000;

    private final URI sseUri;
    private final Map<String, String> headers;
    private final int readTimeout;
    private final HttpClient client;
    /** 服务端推送的消息（JSON-RPC 响应/通知），由读取线程投递、请求线程按 id 取用 */
    private final BlockingQueue<JsonNode> inbox = new LinkedBlockingQueue<>();
    private final AtomicInteger sequence = new AtomicInteger(1);
    private final Object endpointMonitor = new Object();

    private volatile HttpResponse<InputStream> response;
    private volatile BufferedReader reader;
    private volatile Thread readerThread;
    private volatile boolean endpointReady;
    private volatile String endpointError;
    private volatile boolean closed;
    /** 补全后（带代理前缀）的消息端点；POST 命中后固定使用 */
    private volatile String messageUrl;
    /** 标准 URI 解析得到的消息端点：前缀优先时作为退回方案 */
    private volatile String messageUrlStandard;
    /** 该地址是否为流式 HTTP 传输：由连接探测决定（GET 拿不到事件流就按流式 HTTP 走） */
    private volatile boolean streamable;
    /** 流式 HTTP 的会话标识 */
    private volatile String sessionId;
    /** 协商出的协议版本：流式 HTTP 的后续请求要带上它 */
    private volatile String negotiatedVersion;
    private JsonNode initResult;

    public McpHttpClient(String url, Map<String, String> headers, int connectTimeout, int readTimeout) {
        if (DPUtil.empty(url)) throw new IllegalArgumentException("服务地址异常");
        this.sseUri = URI.create(DPUtil.trim(url));
        this.headers = null == headers ? new LinkedHashMap<>() : headers;
        this.readTimeout = readTimeout > 0 ? readTimeout : 60000;
        long connect = connectTimeout > 0 ? connectTimeout : 10000;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connect)).build();
    }

    /**
     * 建立连接并完成初始化握手（initialize + notifications/initialized），返回 initialize 结果
     */
    public JsonNode initialize() throws Exception {
        connect();
        ObjectNode clientInfo = DPUtil.objectNode();
        clientInfo.put("name", "fs-agent-tool");
        clientInfo.put("version", "1.0.0");
        ObjectNode params = DPUtil.objectNode();
        params.put("protocolVersion", streamable ? STREAMABLE_PROTOCOL_VERSION : SSE_PROTOCOL_VERSION);
        params.set("capabilities", DPUtil.objectNode());
        params.set("clientInfo", clientInfo);
        initResult = request("initialize", params);
        // 服务端回应的才是最终协议版本：流式 HTTP 的后续请求要按它带上版本头
        negotiatedVersion = DPUtil.trim(initResult.at("/protocolVersion").asText(""));
        if (DPUtil.empty(negotiatedVersion)) {
            negotiatedVersion = streamable ? STREAMABLE_PROTOCOL_VERSION : SSE_PROTOCOL_VERSION;
        }
        notify("notifications/initialized", null);
        return initResult;
    }

    /** 服务端能力：没有 tools 能力说明这个服务不提供工具方法 */
    public JsonNode serverCapabilities() {
        return null == initResult ? DPUtil.objectNode() : initResult.at("/capabilities");
    }

    public JsonNode serverInfo() {
        return null == initResult ? DPUtil.objectNode() : initResult.at("/serverInfo");
    }

    public String protocolVersion() {
        return null == initResult ? "" : initResult.at("/protocolVersion").asText("");
    }

    /** tools/list：按 nextCursor 取全量 */
    public ArrayNode listTools() throws Exception {
        return collect("tools/list", "tools");
    }

    public ArrayNode listResources() throws Exception {
        return collect("resources/list", "resources");
    }

    public ArrayNode listPrompts() throws Exception {
        return collect("prompts/list", "prompts");
    }

    /** tools/call：返回服务端原始结果（含 content / isError / structuredContent） */
    public JsonNode callTool(String name, Map<String, Object> arguments) throws Exception {
        ObjectNode params = DPUtil.objectNode();
        params.put("name", DPUtil.parseString(name));
        params.set("arguments", DPUtil.toJSON(null == arguments ? new LinkedHashMap<>() : arguments));
        return request("tools/call", params);
    }

    @Override
    public void close() {
        closed = true;
        // 先关底层流：读取线程若正阻塞在 readLine 上会立刻退出。
        // 不能先关 BufferedReader —— SSE 是长连接，读取线程往往一直阻塞在读取上并持有解码器锁，
        // 从别的线程关它会一直等锁，表现为同步请求处理完了却卡在释放资源这一步。
        closeStream(response);
        Thread thread = readerThread;
        if (null != thread && thread != Thread.currentThread()) {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (null == thread || !thread.isAlive()) {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
        }
    }

    /* ------------------------------- 连接与端点 ------------------------------- */

    private void connect() throws Exception {
        HttpResponse<InputStream> probe;
        try {
            probe = client.send(sseRequest(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("MCP 连接失败：" + describe(e) + "（" + sseUri + "）", e);
        }
        int status = probe.statusCode();
        String contentType = DPUtil.parseString(probe.headers().firstValue("content-type").orElse(""));
        if (status < 200 || status >= 300 || contentType.indexOf("text/event-stream") < 0) {
            // 该地址不是 SSE 端点：按流式 HTTP 处理 —— POST 到同一地址，结果直接在响应体里返回
            // （高德 https://mcp.amap.com/mcp 就是这样：GET 返回 405 Method not allowed）
            closeStream(probe);
            useStreamable();
            return;
        }
        response = probe;
        readerThread = new Thread(this::readStream, "mcp-sse-reader");
        readerThread.setDaemon(true);
        readerThread.start();
        long deadline = System.currentTimeMillis() + Math.min(readTimeout, ENDPOINT_TIMEOUT);
        synchronized (endpointMonitor) {
            while (!endpointReady && System.currentTimeMillis() < deadline) endpointMonitor.wait(200);
        }
        if (!endpointReady) {
            // 事件流开着却没下发消息端点：多半是流式 HTTP 服务对 GET 回了一条流，改走流式 HTTP
            closeStream(response);
            useStreamable();
            return;
        }
        if (!DPUtil.empty(endpointError)) throw new IllegalStateException(endpointError);
    }

    /** 退回流式 HTTP：消息端点就是用户填的地址本身，没有事件流可读 */
    private void useStreamable() {
        streamable = true;
        messageUrl = sseUri.toString();
        messageUrlStandard = messageUrl;
    }

    private HttpRequest sseRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder(sseUri)
                .timeout(Duration.ofMillis(readTimeout))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header(PROTOCOL_VERSION_HEADER, SSE_PROTOCOL_VERSION)
                .GET();
        headers.forEach(builder::header);
        return builder.build();
    }

    /** 关闭底层事件流：读取线程会立刻从阻塞的 readLine 退出 */
    private void closeStream(HttpResponse<InputStream> target) {
        try {
            if (null != target) target.body().close();
        } catch (Exception ignored) {
        }
    }

    private void readStream() {
        StringBuilder data = new StringBuilder();
        String event = null;
        try {
            reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
            String line;
            while (null != (line = reader.readLine())) {
                if (line.isEmpty()) {
                    dispatch(event, data.toString());
                    event = null;
                    data.setLength(0);
                    continue;
                }
                if (line.startsWith(":")) continue;
                if (line.startsWith("event:")) {
                    event = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    if (data.length() > 0) data.append('\n');
                    data.append(line.substring(5).trim());
                }
            }
            dispatch(event, data.toString());
        } catch (Exception e) {
            // 连接关闭（正常 close）或读取失败：由下面的 finally 唤醒等待方
        } finally {
            synchronized (endpointMonitor) {
                if (!endpointReady) endpointError = "SSE 连接已关闭，未获取到消息端点：" + sseUri;
                endpointReady = true;
                endpointMonitor.notifyAll();
            }
        }
    }

    private void dispatch(String event, String data) {
        if (DPUtil.empty(data)) return;
        if ("endpoint".equals(event)) {
            resolveMessageUrl(data);
            return;
        }
        JsonNode node = DPUtil.parseJSON(data, key -> null);
        if (null == node || !node.isObject()) return;
        try {
            inbox.put(node);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 解析服务端下发的消息端点：
     * 相对路径按 SSE 地址的父路径补全（这样 {@code http://host/api/agent/sse} 会得到
     * {@code http://host/api/agent/mcp/message}），标准解析结果留作退回方案。
     */
    private void resolveMessageUrl(String endpoint) {
        String raw = DPUtil.trim(endpoint);
        String prefixed = raw;
        if (raw.indexOf("://") < 0) {
            String path = sseUri.getPath();
            int index = path.lastIndexOf('/');
            path = index < 0 ? "/" : path.substring(0, index + 1);
            String origin = sseUri.getScheme() + "://" + sseUri.getAuthority();
            prefixed = raw.startsWith("/") ? origin + path + raw.substring(1) : origin + path + raw;
        }
        messageUrl = prefixed;
        messageUrlStandard = sseUri.resolve(raw).toString();
        synchronized (endpointMonitor) {
            endpointReady = true;
            endpointMonitor.notifyAll();
        }
    }

    /* ------------------------------- 请求 ------------------------------- */

    /** 消息端点候选：优先补全前缀的地址，退回标准解析地址；命中后只留命中的那个 */
    private List<String> targets() {
        List<String> targets = new ArrayList<>();
        if (!DPUtil.empty(messageUrl)) targets.add(messageUrl);
        if (!DPUtil.empty(messageUrlStandard) && !messageUrlStandard.equals(messageUrl)) targets.add(messageUrlStandard);
        return targets;
    }

    /**
     * 发送 JSON-RPC 消息，返回响应体里的内联响应。
     *
     * SSE 传输：结果走事件流，这里一般拿到空响应体；请求头与原 SDK 一致（带 MCP-Protocol-Version、
     * POST 不带 Accept，避免服务端按内容协商把 POST 响应改成 SSE 长连接）。
     * 流式 HTTP：结果就在响应体里，必须声明两种 Accept，并在初始化后回传会话与协议版本。
     */
    private JsonNode post(String json) throws Exception {
        Exception failure = null;
        List<String> targets = targets();
        if (targets.isEmpty()) throw new IllegalStateException("未获取到 MCP 消息端点：" + sseUri);
        for (String target : targets) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target))
                        .timeout(Duration.ofMillis(readTimeout))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
                if (streamable) {
                    builder.header("Accept", STREAMABLE_ACCEPT);
                    if (!DPUtil.empty(sessionId)) builder.header(SESSION_HEADER, sessionId);
                    if (!DPUtil.empty(negotiatedVersion)) builder.header(PROTOCOL_VERSION_HEADER, negotiatedVersion);
                } else {
                    builder.header(PROTOCOL_VERSION_HEADER, SSE_PROTOCOL_VERSION);
                }
                headers.forEach(builder::header);
                HttpResponse<String> result = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                // 流式 HTTP 的会话标识由服务端在 initialize 响应头里下发，后续请求要回传
                if (streamable && DPUtil.empty(sessionId)) {
                    sessionId = DPUtil.trim(result.headers().firstValue(SESSION_HEADER).orElse(""));
                }
                if (result.statusCode() >= 200 && result.statusCode() < 300) {
                    messageUrl = target;
                    messageUrlStandard = target;
                    return inline(result.body());
                }
                String detail = errorDetail(result.body());
                failure = new IllegalStateException("POST " + target + " 返回 HTTP " + result.statusCode()
                        + (DPUtil.empty(detail) ? "" : "：" + detail));
            } catch (Exception e) {
                failure = e;
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }
        if (null == failure) throw new IllegalStateException("MCP 消息端点不可用：" + sseUri);
        throw new IllegalStateException("消息端点请求失败：" + describe(failure) + "（" + targets.get(0) + "）", failure);
    }

    /**
     * POST 响应体里的 JSON-RPC 消息：兼容空体（结果走 SSE 流）、纯 JSON、以及用 SSE 帧包装三种形态。
     * 少数实现（含部分反向代理）不把结果推到 SSE 流，而是直接放在 POST 响应里，这里一并接住。
     */
    private JsonNode inline(String body) {
        String text = DPUtil.trim(DPUtil.parseString(body));
        if (DPUtil.empty(text)) return null;
        if (text.startsWith("{")) return DPUtil.parseJSON(text, key -> null);
        JsonNode node = null;
        for (String line : text.split("\n")) {
            String value = DPUtil.trim(line);
            if (!value.startsWith("data:")) continue;
            node = DPUtil.parseJSON(DPUtil.trim(value.substring(5)), key -> null);
        }
        return null != node && node.isObject() ? node : null;
    }

    /** 响应体里的 JSON-RPC 错误信息：接口拒绝时（如 key 无效）把它带进提示，便于排查 */
    private String errorDetail(String body) {
        JsonNode node = inline(body);
        if (null == node || !node.isObject()) return null;
        String message = DPUtil.trim(node.at("/error/message").asText(""));
        return DPUtil.empty(message) ? null : message;
    }

    /** 取异常链最内层的原因文案：空消息（如连接被拒的 ClosedChannelException）退回类名，便于页面提示 */
    private String describe(Throwable throwable) {
        Throwable cause = throwable;
        while (null != cause.getCause() && cause.getCause() != cause) cause = cause.getCause();
        String message = DPUtil.trim(DPUtil.parseString(cause.getMessage()));
        return DPUtil.empty(message) ? cause.getClass().getSimpleName() : message;
    }

    private JsonNode request(String method, JsonNode params) throws Exception {
        int id = sequence.getAndIncrement();
        ObjectNode body = DPUtil.objectNode();
        body.put("jsonrpc", "2.0");
        body.put("id", id);
        body.put("method", method);
        if (null != params) body.set("params", params);
        JsonNode matched = match(post(DPUtil.stringify(body)), id);
        if (null != matched) return matched;
        // 流式 HTTP 没有事件流：结果一定会出现在响应体里，等下去只会空等到超时
        if (streamable) throw new IllegalStateException("MCP 服务未在响应中返回结果：" + method);
        long deadline = System.currentTimeMillis() + readTimeout;
        while (true) {
            long wait = deadline - System.currentTimeMillis();
            if (wait <= 0) break;
            JsonNode node = inbox.poll(wait, TimeUnit.MILLISECONDS);
            if (null == node) {
                if (closed) break;
                continue;
            }
            matched = match(node, id);
            if (null != matched) return matched;
        }
        throw new IllegalStateException("等待 MCP 响应超时（" + readTimeout + "ms）：" + method);
    }

    /** 命中本次请求 id 时返回结果；服务端报错则抛出；服务端通知或其它响应返回 null */
    private JsonNode match(JsonNode node, int id) {
        if (null == node || !node.isObject() || node.path("id").asInt(-1) != id) return null;
        if (node.has("error")) {
            throw new IllegalStateException("MCP 响应异常：" + node.at("/error/message").asText(node.at("/error").toString()));
        }
        return node.at("/result");
    }

    private void notify(String method, JsonNode params) throws Exception {
        ObjectNode body = DPUtil.objectNode();
        body.put("jsonrpc", "2.0");
        body.put("method", method);
        if (null != params) body.set("params", params);
        post(DPUtil.stringify(body));
    }

    private ArrayNode collect(String method, String field) throws Exception {
        ArrayNode result = DPUtil.arrayNode();
        String cursor = null;
        for (int index = 0; index < MAX_PAGES; index++) {
            ObjectNode params = DPUtil.objectNode();
            if (!DPUtil.empty(cursor)) params.put("cursor", cursor);
            JsonNode page = request(method, params.isEmpty() ? null : params);
            JsonNode items = page.at("/" + field);
            if (items.isArray()) items.forEach(item -> result.add(item));
            cursor = page.at("/nextCursor").asText("");
            if (DPUtil.empty(cursor)) break;
        }
        return result;
    }

}
