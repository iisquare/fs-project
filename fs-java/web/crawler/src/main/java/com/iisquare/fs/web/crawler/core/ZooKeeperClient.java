package com.iisquare.fs.web.crawler.core;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.zookeeper.util.ZookeeperUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ZooKeeperClient implements Closeable {

    public static final Charset charset = StandardCharsets.UTF_8;
    private final CuratorFramework client;
    private final CuratorCache cache;
    private final String nodeId;

    public ZooKeeperClient(String host, int timeout, String nodeId) {
        this.nodeId = nodeId;
        String[] hosts = DPUtil.explode("/", host);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder().connectString(hosts[0]);
        if (hosts.length > 1) builder.namespace(hosts[1]);
        builder.retryPolicy(new ExponentialBackoffRetry(1000, 3)).connectionTimeoutMs(1000).sessionTimeoutMs(timeout);
        client = builder.build();
        cache = CuratorCache.build(client, "/runtime");
    }

    public void listen(WatchListener listener) {
        if (null != listener) cache.listenable().addListener(listener);
    }

    public String nodeId() {
        return nodeId;
    }

    public boolean save(String path, String data) {
        try {
            Stat stat = client.checkExists().forPath(path);
            if (null == data) {
                if (null != stat) {
                    client.delete().forPath(path);
                }
            } else {
                if (null == stat) {
                    path = client.create().creatingParentsIfNeeded().forPath(path);
                }
                client.setData().forPath(path, data.getBytes(charset));
            }
            return true;
        } catch (Exception e) {
            log.warn("save {} failed", path, e);
            return false;
        }
    }

    public Map<String, ZooNode> spiders() {
        Map<String, ZooNode> data = new LinkedHashMap<>();
        List<ChildData> children = ZookeeperUtil.children(cache, "/runtime/spiders/", false);
        for (ChildData child : children) {
            data.put(child.getPath(), ZooNode.decode(new String(child.getData(), charset)));
        }
        return data;
    }

    public void open() {
        FileUtil.close(this);
        ZooNode node = ZooNode.record(nodeId);
        client.start();
        try {
            client.blockUntilConnected();
        } catch (InterruptedException e) {
            log.warn("zookeeper start interrupted", e);
        }
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                    .forPath("/runtime/crawlers/" + node.id, ZooNode.encode(node).getBytes(charset));
        } catch (Exception e) {
            log.warn("register crawler failed", e);
        }
        try {
            cache.start();
        } catch (Exception e) {
            log.warn("watch start failed", e);
        }
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(client, cache);
    }

}
