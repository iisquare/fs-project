package com.iisquare.fs.web.crawler.core;

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
        return DPUtil.stringify(DPUtil.toJSON(notice));
    }

    public static Notice decode(String notice) {
        return DPUtil.toJSON(DPUtil.parseJSON(notice), Notice.class);
    }

}
