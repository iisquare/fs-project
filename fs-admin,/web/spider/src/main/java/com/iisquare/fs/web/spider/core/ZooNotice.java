package com.iisquare.fs.web.spider.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ZooNotice {

    public String action;
    public JsonNode args;
    public String from; // 通过publish方法自动设置
    public long time; // 通过publish方法自动设置

    public static String encode(ZooNotice notice) {
        return DPUtil.stringify(DPUtil.toJSON(notice));
    }

    public static ZooNotice decode(String notice) {
        return DPUtil.toJSON(DPUtil.parseJSON(notice), ZooNotice.class);
    }

    public static ZooNotice startNode(String targetNodeId) {
        ZooNotice notice = new ZooNotice();
        notice.action = "startNode";
        notice.args = DPUtil.objectNode().put("nodeId", targetNodeId);
        return notice;
    }

    public static ZooNotice stopNode(String targetNodeId) {
        ZooNotice notice = new ZooNotice();
        notice.action = "stopNode";
        notice.args = DPUtil.objectNode().put("nodeId", targetNodeId);
        return notice;
    }

}
