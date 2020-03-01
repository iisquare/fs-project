package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class History {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_STOP = "STOP";
    public static final String STATUS_PAUSE = "PAUSE";
    public static final String STATUS_FINISHED = "FINISHED";

    private String scheduleId;
    private long dispatch;
    private long version;
    private String status;
    private int token; // schedule.maxThread
    private int limit; // schedule.maxPerNode

    public static String encode(History history) {
        return DPUtil.stringify(DPUtil.convertJSON(history));
    }

    public static History decode(String history) {
        return DPUtil.convertJSON(DPUtil.parseJSON(history), History.class);
    }

    public static History record(Schedule schedule) {
        if (null == schedule) return null;
        long time = System.currentTimeMillis();
        History history = new History();
        history.scheduleId = schedule.getId();
        history.dispatch = time;
        history.version = time;
        history.status = STATUS_RUNNING;
        history.token = schedule.getMaxThread();
        history.limit = schedule.getMaxPerNode();
        return history;
    }

}
