package com.iisquare.fs.web.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;

import java.io.Closeable;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PlaywrightWorker implements Closeable {

    private final String name;
    private final JsonNode config;
    private final Set<String> blockResources;
    private final List<String> launchArgs;
    private final ExecutorService executor;
    private volatile Playwright playwright;
    private volatile Browser browser;
    private volatile boolean alive = true;

    public PlaywrightWorker(String nodeId, int index, JsonNode config,
                            Set<String> blockResources, List<String> launchArgs) {
        this.name = nodeId + "-" + index;
        this.config = config;
        this.blockResources = blockResources;
        this.launchArgs = launchArgs;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "playwright-" + name);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start(long timeout) throws Exception {
        executor.submit(this::initialize).get(timeout, TimeUnit.MILLISECONDS);
    }

    private synchronized void initialize() {
        if (null != browser) return;
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(config.at("/headless").asBoolean(true))
                .setArgs(new ArrayList<>(launchArgs)));
    }

    public Future<ObjectNode> submit(ObjectNode request) {
        return executor.submit(() -> render(request));
    }

    public boolean isAlive() {
        return alive;
    }

    private ObjectNode render(ObjectNode request) {
        initialize();
        long started = System.currentTimeMillis();
        int timeout = Math.max(1000, request.at("/timeout").asInt(30000));
        String url = DPUtil.trim(request.at("/url").asText());
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("仅支持 HTTP/HTTPS 页面地址");
        }
        String method = request.at("/requestType").asText("GET").toUpperCase(Locale.ROOT);
        Map<String, String> headers = toStringMap(request.at("/headers"));
        Map<String, String> payloadForm = toStringMap(request.at("/payloadForm"));
        String charsetName = request.at("/charset").asText("UTF-8");
        Charset charset = Charset.forName(charsetName);
        if ("GET".equals(method) && !payloadForm.isEmpty()) {
            url = appendQuery(url, payloadForm, charset);
        }
        String targetUrl = url;

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setLocale(DPUtil.empty(config.at("/locale").asText()) ? null : config.at("/locale").asText())
                .setTimezoneId(DPUtil.empty(config.at("/timezone").asText()) ? null : config.at("/timezone").asText())
                .setUserAgent(config.at("/userAgent").asText())
                .setIgnoreHTTPSErrors(config.at("/ignoreHttpsErrors").asBoolean(false))
                .setExtraHTTPHeaders(headers);
        JsonNode proxyData = request.at("/proxy");
        if (proxyData.isObject() && !DPUtil.empty(proxyData.at("/host").asText())) {
            String protocol = proxyData.at("/protocol").asText("http");
            Proxy proxy = new Proxy(protocol + "://" + proxyData.at("/host").asText()
                    + ":" + proxyData.at("/port").asInt());
            if (!DPUtil.empty(proxyData.at("/username").asText())) {
                proxy.setUsername(proxyData.at("/username").asText());
                proxy.setPassword(proxyData.at("/password").asText(""));
            }
            contextOptions.setProxy(proxy);
        }
        Path storageStateFile = null;
        String storageState = request.at("/storageState").asText("");
        if (!DPUtil.empty(storageState) && !"null".equals(storageState)) {
            try {
                storageStateFile = Files.createTempFile("fs-spider-state-", ".json");
                Files.writeString(storageStateFile, storageState, StandardCharsets.UTF_8);
                contextOptions.setStorageStatePath(storageStateFile);
            } catch (Exception e) {
                throw new IllegalStateException("载入浏览器状态失败", e);
            }
        }
        String contentType = normalizedContentType(request.at("/contentType").asText("none"), charset);
        try (BrowserContext context = browser.newContext(contextOptions)) {
            context.route("**/*", route -> route(route, method, targetUrl, request,
                    payloadForm, contentType, charset));
            Page page = context.newPage();
            page.setDefaultTimeout(timeout);
            Page.NavigateOptions navigateOptions = new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(timeout);
            Response response = page.navigate(targetUrl, navigateOptions);
            String warning = waitFor(page, request, timeout);
            String html = page.content();
            int maxHtmlBytes = Math.max(1024, config.at("/maxHtmlBytes").asInt(10485760));
            if (html.getBytes(StandardCharsets.UTF_8).length > maxHtmlBytes) {
                throw new IllegalStateException("渲染后的 HTML 超过大小限制");
            }
            ObjectNode storage = DPUtil.objectNode();
            storage.put("status", null == response ? 0 : response.status());
            storage.put("html", html);
            storage.put("media", "text/html");
            storage.put("location", page.url());
            storage.put("coast", System.currentTimeMillis() - started);
            storage.put("exception", "");
            storage.put("warning", warning);
            storage.put("browser", name);
            storage.put("storageState", context.storageState());
            return storage;
        } catch (RuntimeException e) {
            throw e;
        } finally {
            if (null != storageStateFile) {
                try {
                    Files.deleteIfExists(storageStateFile);
                } catch (Exception ignored) {}
            }
        }
    }

    private void route(Route route, String method, String targetUrl, ObjectNode request,
                       Map<String, String> payloadForm, String contentType, Charset charset) {
        Request browserRequest = route.request();
        if (!"GET".equals(method) && browserRequest.isNavigationRequest()
                && targetUrl.equals(browserRequest.url())) {
            try {
                APIRequestContext api = route.request().frame().page().context().request();
                RequestOptions options = RequestOptions.create().setMethod(method);
                toStringMap(request.at("/headers")).forEach(options::setHeader);
                if (!DPUtil.empty(contentType) && !"multipart/form-data".equals(contentType)) {
                    Map<String, String> requestHeaders = withContentType(
                            toStringMap(request.at("/headers")), contentType);
                    requestHeaders.forEach(options::setHeader);
                }
                if ("x-www-form-urlencoded".equals(contentType)) {
                    FormData form = FormData.create();
                    payloadForm.forEach(form::set);
                    options.setForm(form);
                } else if ("form-data".equals(contentType)) {
                    FormData form = FormData.create();
                    payloadForm.forEach(form::set);
                    options.setMultipart(form);
                } else {
                    String body = request.at("/requestBody").asText("");
                    if (!body.isEmpty()) options.setData(body);
                }
                APIResponse response = api.fetch(targetUrl, options);
                route.fulfill(new Route.FulfillOptions().setResponse(response));
                return;
            } catch (Exception e) {
                route.abort();
                throw new IllegalStateException("浏览器页面请求失败", e);
            }
        }
        String resourceType = browserRequest.resourceType();
        if (blockResources.contains(resourceType)) {
            route.abort();
        } else {
            route.resume();
        }
    }

    private String waitFor(Page page, ObjectNode request, int timeout) {
        String warning = "";
        LoadState waitUntil = loadState(request.at("/waitUntil").asText("networkidle"));
        try {
            page.waitForLoadState(waitUntil, new Page.WaitForLoadStateOptions().setTimeout(timeout));
        } catch (RuntimeException e) {
            if (waitUntil != LoadState.NETWORKIDLE) throw e;
            warning = "networkidle 等待超时，已返回当前渲染内容";
        }
        String selector = DPUtil.trim(request.at("/waitSelector").asText());
        if (!DPUtil.empty(selector)) {
            waitForSelector(page, selector, timeout);
        }
        if (request.at("/scroll").asBoolean(false)) {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        }
        long settle = Math.max(0, config.at("/settleTime").asInt(300));
        if (settle > 0) page.waitForTimeout(settle);
        return warning;
    }

    private void waitForSelector(Page page, String selector, int timeout) {
        page.waitForFunction("selector => !!document.querySelector(selector)", selector,
                new Page.WaitForFunctionOptions().setTimeout(timeout));
    }

    private LoadState loadState(String value) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "load":
                return LoadState.LOAD;
            case "networkidle":
                return LoadState.NETWORKIDLE;
            case "domcontentloaded":
            default:
                return LoadState.DOMCONTENTLOADED;
        }
    }

    private String normalizedContentType(String contentType, Charset charset) {
        switch (contentType) {
            case "json":
                return "application/json; charset=" + charset.name();
            case "xml":
                return "application/xml; charset=" + charset.name();
            case "raw":
                return "text/plain; charset=" + charset.name();
            case "x-www-form-urlencoded":
                return "application/x-www-form-urlencoded; charset=" + charset.name();
            case "form-data":
                return "multipart/form-data";
            default:
                return "";
        }
    }

    private Map<String, String> withContentType(Map<String, String> headers, String contentType) {
        Map<String, String> result = new LinkedHashMap<>(headers);
        boolean present = result.keySet().stream().anyMatch("Content-Type"::equalsIgnoreCase);
        if (!present) result.put("Content-Type", contentType);
        return result;
    }

    private Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();
        if (null != node && node.isObject()) {
            node.fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asText("")));
        }
        return result;
    }

    private String appendQuery(String url, Map<String, String> params, Charset charset) {
        int anchor = url.indexOf('#');
        String fragment = anchor < 0 ? "" : url.substring(anchor);
        String base = anchor < 0 ? url : url.substring(0, anchor);
        StringBuilder target = new StringBuilder(base);
        char separator = base.contains("?") ? '&' : '?';
        for (Map.Entry<String, String> entry : params.entrySet()) {
            target.append(separator);
            separator = '&';
            target.append(URLEncoder.encode(entry.getKey(), charset))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), charset));
        }
        return target.append(fragment).toString();
    }

    @Override
    public synchronized void close() {
        alive = false;
        executor.submit(() -> {
            if (null != browser) {
                browser.close();
                browser = null;
            }
            if (null != playwright) {
                playwright.close();
                playwright = null;
            }
        });
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();
    }
}
