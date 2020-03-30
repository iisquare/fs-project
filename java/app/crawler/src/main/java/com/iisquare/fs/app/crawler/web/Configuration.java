package com.iisquare.fs.app.crawler.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Configuration implements Closeable {

    private static Configuration instance = null;
    private Map<String, Closeable> closeables = new ConcurrentHashMap<>();
    private JsonNode config;

    private Configuration() {}

    public String serverHost() {
        return config.at("/crawler/server/host").asText("127.0.0.1");
    }

    public int serverPort() {
        return config.at("/crawler/server/port").asInt(8433);
    }

    public String nodeName() {
        return serverHost() + ":" + serverPort();
    }

    public JsonNode config() {
        return this.config;
    }

    public JsonNode config(JsonNode config) {
        if (null == config) config = DPUtil.objectNode();
        return this.config = config;
    }

    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = new Configuration();
                }
            }
        }
        return instance;
    }

    public void defer(Closeable instance) {
        closeables.put(instance.getClass().getName(), instance);
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<String, Closeable> entry : closeables.entrySet()) {
            entry.getValue().close();
        }
    }
}
