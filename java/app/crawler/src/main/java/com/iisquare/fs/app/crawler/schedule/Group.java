package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Group {

    private String name;
    private String target; // 限制目标[broadcast-默认全部， node-每个节点]
    private int interval; // 并发间隔
    private int concurrent; // 并发数量，通过Redis.incr(ttl)进行限流

    public static String encode(Group group) {
        return DPUtil.stringify(DPUtil.convertJSON(group));
    }

    public static Group decode(String group) {
        return DPUtil.convertJSON(DPUtil.parseJSON(group), Group.class);
    }

}
