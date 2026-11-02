package com.iisquare.fs.web.browser;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BrowserZooKeeper implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(BrowserZooKeeper.class);
    private final CuratorFramework client;
    private final String nodeId;
    private final String path;
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "browser-zk-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private volatile BrowserNode node;
    private volatile PlaywrightPool pool;

    public BrowserZooKeeper(String connectString, int timeout, String nodeId) {
        String[] hosts = DPUtil.explode("/", connectString);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(hosts[0])
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectionTimeoutMs(1000)
                .sessionTimeoutMs(timeout);
        if (hosts.length > 1) builder.namespace(hosts[1]);
        this.client = builder.build();
        this.nodeId = nodeId;
        this.path = "/runtime/browsers/" + nodeId;
    }

    public void open() throws Exception {
        client.start();
        client.blockUntilConnected();
        client.getConnectionStateListenable().addListener((ignored, state) -> {
            switch (state) {
                case CONNECTED:
                case RECONNECTED:
                    register();
                    break;
                default:
                    break;
            }
        });
    }

    public void watchPool(PlaywrightPool pool) {
        this.pool = pool;
        this.node = new BrowserNode(nodeId, pool.maxConcurrency());
        register();
        heartbeat.scheduleWithFixedDelay(this::publishLoad, 1, 1, TimeUnit.SECONDS);
    }

    private synchronized void register() {
        if (null == node || null == pool) return;
        try {
            Stat stat = client.checkExists().forPath(path);
            byte[] payload = DPUtil.stringify(node.toJson(pool)).getBytes(StandardCharsets.UTF_8);
            if (null == stat) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, payload);
            } else {
                client.setData().forPath(path, payload);
            }
        } catch (Exception e) {
            log.warn("register browser node {} failed", nodeId, e);
        }
    }

    private void publishLoad() {
        if (null == node || null == pool) return;
        try {
            client.setData().forPath(path,
                    DPUtil.stringify(node.toJson(pool)).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("publish browser load failed", e);
        }
    }

    public String nodeId() {
        return nodeId;
    }

    @Override
    public void close() {
        heartbeat.shutdownNow();
        FileUtil.close(client);
    }
}
