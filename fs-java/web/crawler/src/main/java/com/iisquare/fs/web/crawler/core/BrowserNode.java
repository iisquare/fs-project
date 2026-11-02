package com.iisquare.fs.web.crawler.core;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BrowserNode {

    private String id;
    private String host;
    private Integer port;
    private String role;
    private Integer maxConcurrency;
    private Integer active;
    private Integer queued;
    private Integer available;
    private Long time;
    private Long updatedTime;

    public static BrowserNode decode(String value) {
        return DPUtil.toJSON(DPUtil.parseJSON(value), BrowserNode.class);
    }

    public String endpoint() {
        return "http://" + host + ":" + port;
    }

    public boolean fresh(long maxAge) {
        return null != updatedTime && System.currentTimeMillis() - updatedTime <= maxAge;
    }

    public double load() {
        int capacity = Math.max(1, null == maxConcurrency ? 1 : maxConcurrency);
        int running = Math.max(0, null == active ? 0 : active);
        int waiting = Math.max(0, null == queued ? 0 : queued);
        return (running + waiting * 0.35D) / capacity;
    }
}
