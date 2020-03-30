package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Notice {

    public String type;
    public String action;
    public String from;
    public String content;
    public long time;

    public static String encode(Notice notice) {
        return DPUtil.stringify(DPUtil.convertJSON(notice));
    }

    public static Notice decode(String notice) {
        return DPUtil.convertJSON(DPUtil.parseJSON(notice), Notice.class);
    }

    public static Notice empty() {
        Notice notice = new Notice();
        notice.time = System.currentTimeMillis();
        return notice;
    }

    public static Notice proxyChange() {
        Notice notice = empty();
        notice.type = "proxy";
        notice.action = "change";
        return notice;
    }

    public static Notice clearCounter(String scheduleId) {
        Notice notice = empty();
        notice.type = "schedule";
        notice.action = "clearCounter";
        notice.content = scheduleId;
        return notice;
    }

}
