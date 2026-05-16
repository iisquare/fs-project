package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Proxy {

    private String name;
    private String target; // 限制目标[broadcast-默认全部， nodeId-具体节点]
    private String schema;
    private String host;
    private int port;
    private int connectTimeout;
    private int socketTimeout;

    public static String encode(Proxy proxy) {
        return DPUtil.stringify(DPUtil.toJSON(proxy));
    }

    public static Proxy decode(String proxy) {
        return DPUtil.toJSON(DPUtil.parseJSON(proxy), Proxy.class);
    }


}
