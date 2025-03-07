package com.iisquare.fs.app.crawler.node;

import com.iisquare.fs.app.crawler.schedule.*;
import com.iisquare.fs.app.crawler.schedule.*;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.app.crawler.schedule.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
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

    private static final Charset charset = Charset.forName("UTF-8");
    private CuratorFramework client;
    protected final static Logger logger = LoggerFactory.getLogger(ZooKeeperClient.class);
    private LeaderLatch latch;
    private TreeCache watch;

    public ZooKeeperClient (String host, int timeout, String nodeId) {
        String[] hosts = DPUtil.explode("/", host);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder().connectString(hosts[0]);
        if (hosts.length > 1) builder.namespace(hosts[1]);
        builder.retryPolicy(new ExponentialBackoffRetry(1000, 3)).connectionTimeoutMs(1000).sessionTimeoutMs(timeout);
        client = builder.build();
        latch = new LeaderLatch(client, "/nodes", nodeId);
        watch = new TreeCache(client, "/runtime");
    }

    public void listen(WatchListener listener) {
        if (null != listener) watch.getListenable().addListener(listener);
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

    public Proxy proxy(String name) {
        if (DPUtil.empty(name)) return null;
        ChildData data = watch.getCurrentData("/runtime/proxy/" + name);
        if (null == data) return null;
        return Proxy.decode(new String(data.getData(), charset));
    }

    public Map<String, Proxy> proxies() {
        Map<String, Proxy> data = new LinkedHashMap<>();
        Map<String, ChildData> map = watch.getCurrentChildren("/runtime/proxy");
        if (null == map) return data;
        for (Map.Entry<String, ChildData> entry : map.entrySet()) {
            data.put(entry.getKey(), Proxy.decode(new String(entry.getValue().getData(), charset)));
        }
        return data;
    }

    public Group group(String name) {
        if (DPUtil.empty(name)) return null;
        ChildData data = watch.getCurrentData("/runtime/group/" + name);
        if (null == data) return null;
        return Group.decode(new String(data.getData(), charset));
    }

    public Map<String, Group> groups() {
        Map<String, Group> data = new LinkedHashMap<>();
        Map<String, ChildData> map = watch.getCurrentChildren("/runtime/group");
        if (null == map) return data;
        for (Map.Entry<String, ChildData> entry : map.entrySet()) {
            data.put(entry.getKey(), Group.decode(new String(entry.getValue().getData(), charset)));
        }
        return data;
    }

    public boolean save(History history) {
        return save("/history/" + history.getScheduleId(), History.encode(history));
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

    public History history(String scheduleId) {
        ChildData data = watch.getCurrentData("/runtime/history/" + scheduleId);
        if (null == data) return null;
        return History.decode(new String(data.getData(), charset));
    }

    public Map<String, History> histories() {
        Map<String, History> data = new LinkedHashMap<>();
        Map<String, ChildData> map = watch.getCurrentChildren("/runtime/history");
        if (null == map) return data;
        for (Map.Entry<String, ChildData> entry : map.entrySet()) {
            data.put(entry.getKey(), History.decode(new String(entry.getValue().getData(), charset)));
        }
        return data;
    }

    public Schedule schedule(String scheduleId) {
        ChildData data = watch.getCurrentData("/runtime/schedule/" + scheduleId);
        if (null == data) return null;
        return Schedule.decode(new String(data.getData(), charset));
    }

    public Map<String, Schedule> schedules() {
        Map<String, Schedule> data = new LinkedHashMap<>();
        Map<String, ChildData> map = watch.getCurrentChildren("/runtime/schedule");
        if (null == map) return data;
        for (Map.Entry<String, ChildData> entry : map.entrySet()) {
            data.put(entry.getKey(), Schedule.decode(new String(entry.getValue().getData(), charset)));
        }
        return data;
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

    @Override
    public void close() throws IOException {
        FileUtil.close(client, latch, watch);
    }

    public boolean publish(Notice notice) {
        if (null == notice) return false;
        notice.from = nodeId();
        return save("/notice", Notice.encode(notice));
    }

}
