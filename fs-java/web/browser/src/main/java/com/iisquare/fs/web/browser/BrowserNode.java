package com.iisquare.fs.web.browser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

public class BrowserNode {

    public final String id;
    public final String host;
    public final int port;
    public final int maxConcurrency;
    public final long startedTime;

    public BrowserNode(String nodeId, int maxConcurrency) {
        String[] parts = nodeId.split(":");
        if (parts.length < 3) throw new IllegalArgumentException("invalid browser node id: " + nodeId);
        this.id = nodeId;
        this.host = parts[0];
        this.port = Integer.parseInt(parts[1]);
        this.maxConcurrency = Math.max(1, maxConcurrency);
        this.startedTime = System.currentTimeMillis();
    }

    public ObjectNode toJson(PlaywrightPool pool) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", id);
        node.put("host", host);
        node.put("port", port);
        node.put("role", "browser");
        node.put("maxConcurrency", maxConcurrency);
        node.put("active", pool.active());
        node.put("queued", pool.queued());
        node.put("available", Math.max(0, maxConcurrency - pool.active()));
        node.put("time", startedTime);
        node.put("updatedTime", System.currentTimeMillis());
        return node;
    }

    public String endpoint() {
        return "http://" + host + ":" + port;
    }
}
