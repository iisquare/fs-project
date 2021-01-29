package com.iisquare.fs.base.redis.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;

@Configuration
public class RedisConfiguration {

    @Autowired
    private StringRedisTemplate redis;

    /**
     * 线程之间默认共享Redis连接句柄
     * 暂不支持通过配置文件定义shareNativeConnection参数：https://github.com/spring-projects/spring-boot/pull/14217
     * 目前只有此处用到Redis，可通过自定义Factory重写该处逻辑：https://github.com/spring-projects/spring-boot/issues/14196
     */
    @PostConstruct
    public void initialize() {
        LettuceConnectionFactory factory = (LettuceConnectionFactory) redis.getConnectionFactory();
        factory.setShareNativeConnection(false);
    }

}
