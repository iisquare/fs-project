package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.app.crawler.node.ZooKeeperClient;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ProxyFactory {

    private ZooKeeperClient zookeeper;
    private WeakHashMap<String, RequestConfig> configs;
    private List<Proxy> proxies = new ArrayList<>(); // 每次变化会重新赋值，不需要CopyOnWriteArrayList

    public ProxyFactory(ZooKeeperClient zookeeper) {
        this.zookeeper = zookeeper;
        configs = new WeakHashMap<>();
    }

    public List<Proxy> proxies(boolean reload) {
        if (!reload) return proxies;
        String node = zookeeper.nodeId();
        List<Proxy> list = new ArrayList<>();
        for (Map.Entry<String, Proxy> entry : zookeeper.proxies().entrySet()) {
            Proxy proxy = entry.getValue();
            String target = proxy.getTarget();
            if ("broadcast".equals(target) || node.equals(target)) {
                list.add(proxy);
            }
        }
        return proxies = list;
    }

    public Proxy proxy() {
        List<Proxy> list = proxies(false);
        int size = list.size();
        if (size < 1) return null;
        if (size < 2) return list.get(0);
        return list.get((int) (System.currentTimeMillis() % size));
    }

    public RequestConfig config() {
        Proxy proxy = proxy();
        if (null == proxy) return null;
        String key = CodeUtil.md5(Proxy.encode(proxy));
        RequestConfig config = configs.get(key);
        if (null == config) {
            RequestConfig.Builder builder = RequestConfig.custom();
            builder.setProxy(new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getSchema()));
            if (proxy.getConnectTimeout() > 0) builder.setConnectTimeout(proxy.getConnectTimeout());
            if (proxy.getSocketTimeout() > 0) builder.setSocketTimeout(proxy.getSocketTimeout());
            config = builder.build();
        }
        return config;
    }

}
