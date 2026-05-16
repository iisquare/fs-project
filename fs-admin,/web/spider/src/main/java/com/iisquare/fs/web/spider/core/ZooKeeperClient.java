package com.iisquare.fs.web.spider.core;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.zookeeper.util.ZookeeperUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class ZooKeeperClient implements Closeable {

    public static final Charset charset = StandardCharsets.UTF_8;
    private final CuratorFramework client;
    private final LeaderLatch latch;
    private final CuratorCache cache;
    private final String nodeId;

    public ZooKeeperClient(String host, int timeout, String nodeId) {
        this.nodeId = nodeId;
        String[] hosts = DPUtil.explode("/", host);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder().connectString(hosts[0]);
        if (hosts.length > 1) builder.namespace(hosts[1]);
        builder.retryPolicy(new ExponentialBackoffRetry(1000, 3)).connectionTimeoutMs(1000).sessionTimeoutMs(timeout);
        client = builder.build();
        latch = new LeaderLatch(client, "/nodes", nodeId);
        cache = CuratorCache.build(client, "/runtime");
    }

    public void listen(WatchListener listener) {
        if (null != listener) cache.listenable().addListener(listener);
    }

    public String nodeId() {
        return latch.getId();
    }

    public String state() {
        return latch.getState().name();
    }

    public boolean isLeader() {
        return latch.hasLeadership();
    }

    public boolean save(ZooRate rate) {
        return save("/runtime/rate/" + rate.getId(), ZooRate.encode(rate));
    }

    public boolean save(ZooJob job) {
        return save("/runtime/job/" + job.getId(), ZooJob.encode(job));
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

    public ZooJob job(String id) {
        ChildData data = cache.get("/runtime/job/" + id).orElse(null);
        if (null == data) return null;
        return ZooJob.decode(new String(data.getData(), charset));
    }

    public Map<String, ZooJob> jobs() {
        Map<String, ZooJob> data = new LinkedHashMap<>();
        List<ChildData> children = ZookeeperUtil.children(cache, "/runtime/job/", false);
        for (ChildData child : children) {
            ZooJob job = ZooJob.decode(new String(child.getData(), charset));
            data.put(job.getId(), job);
        }
        return data;
    }

    public ZooRate rate(String id) {
        ChildData data = cache.get("/runtime/rate/" + id).orElse(null);
        if (null == data) return null;
        return ZooRate.decode(new String(data.getData(), charset));
    }

    public Map<String, ZooRate> rates() {
        Map<String, ZooRate> data = new LinkedHashMap<>();
        List<ChildData> children = ZookeeperUtil.children(cache, "/runtime/rate/", false);
        for (ChildData child : children) {
            ZooRate rate = ZooRate.decode(new String(child.getData(), charset));
            data.put(rate.getId(), rate);
        }
        return data;
    }

    public Map<String, ZooNode> spiders() {
        Map<String, ZooNode> data = new LinkedHashMap<>();
        List<ChildData> children = ZookeeperUtil.children(cache, "/runtime/spiders/", false);
        for (ChildData child : children) {
            data.put(child.getPath(), ZooNode.decode(new String(child.getData(), charset)));
        }
        return data;
    }

    public Map<String, ZooNode> crawlers() {
        Map<String, ZooNode> data = new LinkedHashMap<>();
        List<ChildData> children = ZookeeperUtil.children(cache, "/runtime/crawlers/", false);
        for (ChildData child : children) {
            data.put(child.getPath(), ZooNode.decode(new String(child.getData(), charset)));
        }
        return data;
    }

    public Collection<Participant> participants() {
        try {
            return latch.getParticipants();
        } catch (Exception e) {
            log.warn("latch participants failed", e);
            return null;
        }
    }

    public String leaderId() {
        try {
            return latch.getLeader().getId();
        } catch (Exception e) {
            log.warn("latch leaderId failed", e);
            return null;
        }
    }

    public void open() {
        FileUtil.close(this);
        ZooNode node = ZooNode.record(nodeId);
        client.getConnectionStateListenable().addListener((client, state) -> {
            switch (state) {
                case CONNECTED:
                case RECONNECTED:
                    register(node);
                    break;
            }
        });
        client.start();
        try {
            client.blockUntilConnected();
        } catch (InterruptedException e) {
            log.warn("zookeeper start interrupted", e);
        }
        try {
            latch.start();
        } catch (Exception e) {
            log.warn("latch start failed", e);
        }
        try {
            cache.start();
        } catch (Exception e) {
            log.warn("watch start failed", e);
        }
    }

    public boolean register(ZooNode node) {
        String path = "/runtime/spiders/" + node.id;
        try {
            Stat stat = client.checkExists().forPath(path);
            if (null != stat) return true;
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                    .forPath(path, ZooNode.encode(node).getBytes(charset));
            return true;
        } catch (Exception e) {
            log.warn("register spider failed", e);
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(client, latch, cache);
    }

    public boolean publish(ZooNotice notice) {
        if (null == notice) return false;
        notice.from = nodeId();
        notice.time = System.currentTimeMillis();
        return save("/runtime/notice", ZooNotice.encode(notice));
    }

}
