package com.iisquare.fs.app.flink.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.http.HttpHost;

import java.util.ArrayList;
import java.util.List;

public class ElasticUtil {

    public static List<HttpHost> hosts(String nodes) {
        List<HttpHost> hosts = new ArrayList<>();
        for (String target : DPUtil.explode(",", nodes, " ", true)) {
            String[] strings = DPUtil.explode(":", target, " ", true);
            String host = strings.length > 0 ? strings[0] : "localhost";
            int port = strings.length > 1 ? DPUtil.parseInt(strings[1]) : 9200;
            hosts.add(new HttpHost(host, port, "http"));
        }
        if (hosts.size() < 1) hosts.add(new HttpHost("localhost", 9200, "http"));
        return hosts;
    }

}
