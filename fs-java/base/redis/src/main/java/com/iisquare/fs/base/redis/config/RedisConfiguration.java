package com.iisquare.fs.base.redis.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfiguration implements InitializingBean {

    @Autowired
    StringRedisTemplate redis;

    /**
     * 线程之间默认共享Redis连接句柄
     * 暂不支持通过配置文件定义shareNativeConnection参数：https://github.com/spring-projects/spring-boot/pull/14217
     * 目前只有此处用到Redis，可通过自定义Factory重写该处逻辑：https://github.com/spring-projects/spring-boot/issues/14196
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        RedisConnectionFactory factory = redis.getConnectionFactory();
        // 项目通过 spring-boot-starter-data-redis + Jedis 工作，不能强制假定为 Lettuce。
        if (factory instanceof LettuceConnectionFactory lettuce) {
            lettuce.setShareNativeConnection(false);
        }
    }
}
