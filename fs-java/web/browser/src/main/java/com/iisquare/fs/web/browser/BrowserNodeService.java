package com.iisquare.fs.web.browser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BrowserNodeService implements InitializingBean, DisposableBean {

    @Value("${fs.browser.zookeeper.host}")
    private String zookeeperHost;
    @Value("${fs.browser.zookeeper.timeout:2000}")
    private int zookeeperTimeout;
    @Value("${fs.browser.zookeeper.node-id}")
    private String nodeId;
    @Value("${fs.browser.playwright.headless:true}")
    private boolean headless;
    @Value("${fs.browser.playwright.max-concurrency:2}")
    private int maxConcurrency;
    @Value("${fs.browser.playwright.navigation-timeout:30000}")
    private int navigationTimeout;
    @Value("${fs.browser.playwright.settle-time:300}")
    private int settleTime;
    @Value("${fs.browser.playwright.max-html-bytes:10485760}")
    private int maxHtmlBytes;
    @Value("${fs.browser.playwright.ignore-https-errors:false}")
    private boolean ignoreHttpsErrors;
    @Value("${fs.browser.playwright.locale:zh-CN}")
    private String locale;
    @Value("${fs.browser.playwright.timezone:Asia/Shanghai}")
    private String timezone;
    @Value("${fs.browser.playwright.user-agent}")
    private String userAgent;
    @Value("${fs.browser.playwright.block-resources:image,media,font}")
    private List<String> blockResources;
    @Value("${fs.browser.playwright.launch-args:}")
    private List<String> launchArgs;

    private BrowserZooKeeper zookeeper;
    private PlaywrightPool pool;

    @Override
    public void afterPropertiesSet() throws Exception {
        ObjectNode config = DPUtil.objectNode();
        config.put("headless", headless);
        config.put("maxConcurrency", maxConcurrency);
        config.put("navigationTimeout", navigationTimeout);
        config.put("settleTime", settleTime);
        config.put("maxHtmlBytes", maxHtmlBytes);
        config.put("ignoreHttpsErrors", ignoreHttpsErrors);
        config.put("locale", locale);
        config.put("timezone", timezone);
        config.put("userAgent", userAgent);
        config.set("blockResources", DPUtil.toJSON(blockResources));
        config.set("launchArgs", DPUtil.toJSON(launchArgs));

        zookeeper = new BrowserZooKeeper(zookeeperHost, zookeeperTimeout, nodeId);
        zookeeper.open();
        pool = new PlaywrightPool(config, nodeId);
        zookeeper.watchPool(pool);
    }

    public ObjectNode fetch(ObjectNode request) throws Exception {
        return pool.render(request);
    }

    public ObjectNode state() {
        ObjectNode state = DPUtil.objectNode();
        state.put("nodeId", nodeId);
        state.put("role", "browser");
        state.put("state", "STARTED");
        state.put("maxConcurrency", pool.maxConcurrency());
        state.put("active", pool.active());
        state.put("queued", pool.queued());
        state.put("available", Math.max(0, pool.maxConcurrency() - pool.active()));
        return state;
    }

    @Override
    public void destroy() {
        FileUtil.close(pool, zookeeper);
        pool = null;
        zookeeper = null;
    }
}
