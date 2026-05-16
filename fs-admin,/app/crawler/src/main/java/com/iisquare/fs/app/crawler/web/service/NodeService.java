package com.iisquare.fs.app.crawler.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.node.ZooKeeperClient;
import com.iisquare.fs.app.crawler.schedule.Notice;
import com.iisquare.fs.app.crawler.web.Configuration;
import com.iisquare.fs.app.crawler.web.ServiceBase;
import com.iisquare.fs.app.crawler.node.ZooKeeperClient;
import com.iisquare.fs.app.crawler.schedule.Notice;
import com.iisquare.fs.app.crawler.web.Configuration;
import com.iisquare.fs.app.crawler.web.ServiceBase;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class NodeService extends ServiceBase implements Closeable {

    private static NodeService instance = null;
    private Configuration configuration = Configuration.getInstance();
    private ZooKeeperClient zookeeper;

    private NodeService() {
        configuration.defer(this);
        JsonNode config = configuration.config();
        String host = config.at("/crawler/zookeeper/host").asText("127.0.0.1:2181/crawler");
        int timeout = config.at("/crawler/zookeeper/timeout").asInt(2000);
        zookeeper = new ZooKeeperClient(host, timeout, configuration.nodeName());
        zookeeper.open();
    }

    public ZooKeeperClient zookeeper() {
        return zookeeper;
    }

    public List<String> participants() {
        return zookeeper.participants();
    }

    public void notice(Notice notice) {
        zookeeper.publish(notice);
    }

    public JsonNode state() {
        ObjectNode data = DPUtil.objectNode();
        data.put("id", zookeeper.nodeId());
        data.put("state", zookeeper.state());
        data.put("leader", zookeeper.leaderId());
        data.put("leadership", zookeeper.isLeader());
        return data;
    }

    public static NodeService getInstance() {
        if (instance == null) {
            synchronized (NodeService.class) {
                if (instance == null) {
                    instance = new NodeService();
                }
            }
        }
        return instance;
    }

    @Override
    public void close() throws IOException {
        if (null != zookeeper) zookeeper.close();
    }
}
