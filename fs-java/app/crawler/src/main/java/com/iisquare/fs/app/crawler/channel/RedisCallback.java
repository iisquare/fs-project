package com.iisquare.fs.app.crawler.channel;

import io.lettuce.core.api.StatefulRedisConnection;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class RedisCallback {

    private String name;

    @Override
    public String toString() {
        return "RedisCallback[name=" + name + "]";
    }

    /**
     * 若执行失败返回true，执行成功返回false
     */
    public abstract boolean invoke(StatefulRedisConnection<String, String> connect);

}
