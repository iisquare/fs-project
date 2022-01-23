package com.iisquare.fs.web.cron.core;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZooKeeperClient implements Closeable {

    public static final Charset charset = Charset.forName("UTF-8");
    private CuratorFramework client;
    protected final static Logger logger = LoggerFactory.getLogger(ZooKeeperClient.class);
    private LeaderLatch latch;
    private TreeCache watch;
    private InterProcessMutex lock;

    public ZooKeeperClient(String host, int timeout, String nodeId) {
        String[] hosts = DPUtil.explode(host, "/");
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder().connectString(hosts[0]);
        if (hosts.length > 1) builder.namespace(hosts[1]);
        builder.retryPolicy(new ExponentialBackoffRetry(1000, 3)).connectionTimeoutMs(1000).sessionTimeoutMs(timeout);
        client = builder.build();
        latch = new LeaderLatch(client, "/nodes", nodeId);
        watch = new TreeCache(client, "/runtime");
        lock = new InterProcessMutex(client, "/lock");
    }

    public void listen(WatchListener listener) {
        if (null != listener) watch.getListenable().addListener(listener);
    }

    public InterProcessMutex lock() {
        return this.lock;
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

    public String command() {
        ChildData data = watch.getCurrentData("/runtime/command/" + nodeId());
        if (null == data) return WatchListener.CMD_EMPTY;
        return new String(data.getData(), charset);
    }

    public boolean command(String command) {
        return save("/command/" + nodeId(), command);
    }

    public Map<String, String> commands() {
        Map<String, String> data = new LinkedHashMap<>();
        Map<String, ChildData> map = watch.getCurrentChildren("/runtime/command");
        if (null == map) return data;
        for (Map.Entry<String, ChildData> entry : map.entrySet()) {
            data.put(entry.getKey(), new String(entry.getValue().getData(), charset));
        }
        return data;
    }

    public boolean save(String path, String data) {
        path = "/runtime" + path;
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
            logger.warn("save " + path + " failed", e);
            return false;
        }
    }

    public List<String> participants() {
        List<String> list = new ArrayList<>();
        try {
            for (Participant participant : latch.getParticipants()) {
                list.add(participant.getId());
            }
            return list;
        } catch (Exception e) {
            logger.warn("latch participants failed", e);
            return null;
        }
    }

    public String leaderId() {
        try {
            return latch.getLeader().getId();
        } catch (Exception e) {
            logger.warn("latch leaderId failed", e);
            return null;
        }
    }

    public void open() {
        FileUtil.close(this);
        client.start();
        try {
            client.blockUntilConnected();
        } catch (InterruptedException e) {
            logger.warn("zookeeper start interrupted", e);
        }
        try {
            latch.start();
        } catch (Exception e) {
            logger.warn("latch start failed", e);
        }
        try {
            watch.start();
        } catch (Exception e) {
            logger.warn("watch start failed", e);
        }
    }

    public Exception release(InterProcessMutex lock) {
        if (lock.isAcquiredInThisProcess()) {
            try {
                lock.release();
            } catch (Exception e) {
                return e;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        release(lock);
        FileUtil.close(client, latch, watch);
    }

}
