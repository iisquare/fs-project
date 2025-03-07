package com.iisquare.fs.app.crawler.schedule;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

public class WatchListener implements TreeCacheListener {

    private Scheduler scheduler;

    public WatchListener(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void onNotice(Notice notice) {
        if (null == notice) return;
        switch (notice.type) {
            case "proxy":
                switch (notice.action) {
                    case "change":
                        scheduler.getProxyFactory().proxies(true);
                        break;
                }
                break;
            case "schedule":
                switch (notice.action) {
                    case "clearCounter":
                        scheduler.clearCounter(notice.getContent());
                        break;
                }
                break;
        }
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        ChildData data = event.getData();
        if (null == data) return;
        String path = data.getPath();
        if (path.startsWith("/runtime/proxy/")) {
            onNotice(Notice.proxyChange());
            return;
        }
        byte[] message = data.getData();
        if (path.equals("/runtime/notice")) {
            if (null == message) return;
            onNotice(Notice.decode(new String(message)));
            return;
        }
    }
}
