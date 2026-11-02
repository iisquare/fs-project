package com.iisquare.fs.web.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.microsoft.playwright.PlaywrightException;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class PlaywrightPool implements Closeable {

    private final BlockingQueue<PlaywrightWorker> idle = new LinkedBlockingQueue<>();
    private final List<PlaywrightWorker> workers = new ArrayList<>();
    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger queued = new AtomicInteger();
    private final int maxConcurrency;

    public PlaywrightPool(JsonNode config, String nodeId) throws Exception {
        if (null == config || !config.isObject()) config = DPUtil.objectNode();
        this.maxConcurrency = Math.max(1, config.at("/maxConcurrency").asInt(2));
        Set<String> blockResources = new LinkedHashSet<>();
        if (config.at("/blockResources").isArray()) {
            config.at("/blockResources").forEach(item -> blockResources.add(item.asText()));
        } else {
            blockResources.addAll(Set.of("image", "media", "font"));
        }
        List<String> launchArgs = new ArrayList<>();
        if (config.at("/launchArgs").isArray()) {
            config.at("/launchArgs").forEach(item -> launchArgs.add(item.asText()));
        }
        try {
            for (int i = 0; i < maxConcurrency; i++) {
                PlaywrightWorker worker = new PlaywrightWorker(nodeId, i, config, blockResources, launchArgs);
                worker.start(config.at("/navigationTimeout").asInt(30000) + 10000L);
                workers.add(worker);
                idle.offer(worker);
            }
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    public int maxConcurrency() {
        return maxConcurrency;
    }

    public int active() {
        return active.get();
    }

    public int queued() {
        return queued.get();
    }

    public ObjectNode render(ObjectNode request) throws Exception {
        queued.incrementAndGet();
        PlaywrightWorker worker;
        try {
            worker = idle.poll();
        } finally {
            queued.decrementAndGet();
        }
        if (null == worker) {
            throw new BrowserBusyException("无空闲浏览器实例，请稍后重试");
        }
        active.incrementAndGet();
        Future<ObjectNode> future = worker.submit(request);
        try {
            int timeout = Math.max(1000, request.at("/timeout").asInt(30000));
            return future.get(timeout + 15000L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            worker.close();
            throw new BrowserBusyException("浏览器请求执行超时");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PlaywrightException) throw (PlaywrightException) cause;
            if (cause instanceof Exception) throw (Exception) cause;
            throw new IllegalStateException(cause);
        } finally {
            active.decrementAndGet();
            if (worker.isAlive()) idle.offer(worker);
        }
    }

    @Override
    public void close() {
        for (PlaywrightWorker worker : workers) {
            FileUtil.close(worker);
        }
        idle.clear();
        workers.clear();
    }

    public static class BrowserBusyException extends Exception {
        public BrowserBusyException(String message) {
            super(message);
        }
    }
}
