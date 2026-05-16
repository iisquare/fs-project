package com.iisquare.fs.web.crawler.core;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ZooNode {

    public String id;
    public String host;
    public Integer port;
    public String role;
    public Long time;

    public static String encode(ZooNode node) {
        return DPUtil.stringify(DPUtil.toJSON(node));
    }

    public static ZooNode decode(String node) {
        return DPUtil.toJSON(DPUtil.parseJSON(node), ZooNode.class);
    }

    public String endpoint() {
        return "http://" + host + ":" + port;
    }

    public static ZooNode record(String nodeId) {
        ZooNode node = new ZooNode();
        List<String> strings = DPUtil.matcher("^([0-9\\.\\-a-z]+):(\\d+):([a-z]+)$", nodeId, true);
        node.id = nodeId;
        node.host = strings.get(1);
        node.port = Integer.valueOf(strings.get(2));
        node.role = strings.get(3);
        node.time = System.currentTimeMillis();
        return node;
    }

}
