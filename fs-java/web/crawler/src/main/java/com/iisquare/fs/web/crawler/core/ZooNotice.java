package com.iisquare.fs.web.crawler.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ZooNotice {

    public String from;
    public String action;
    public JsonNode args;
    public long time;

    public static String encode(ZooNotice notice) {
        return DPUtil.stringify(DPUtil.toJSON(notice));
    }

    public static ZooNotice decode(String notice) {
        return DPUtil.toJSON(DPUtil.parseJSON(notice), ZooNotice.class);
    }

}
