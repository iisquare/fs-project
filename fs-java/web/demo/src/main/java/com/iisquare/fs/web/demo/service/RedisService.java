package com.iisquare.fs.web.demo.service;

import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService extends ServiceBase implements InitializingBean {

    @Autowired
    private StringRedisTemplate redis;

    /**
     * 线程之间默认共享Redis连接句柄
     * 暂不支持通过配置文件定义shareNativeConnection参数：https://github.com/spring-projects/spring-boot/pull/14217
     * 目前只有此处用到Redis，可通过自定义Factory重写该处逻辑：https://github.com/spring-projects/spring-boot/issues/14196
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        LettuceConnectionFactory factory = (LettuceConnectionFactory) redis.getConnectionFactory();
        factory.setShareNativeConnection(false);
    }

    private String lockKey(Integer uid) {
        return "fs:demo:lock:" + uid;
    }

    public Boolean lock(Integer uid) {
        ValueOperations<String, String> ops = redis.opsForValue();
        return ops.setIfAbsent(lockKey(uid), String.valueOf(System.currentTimeMillis()), 10000, TimeUnit.MILLISECONDS);
    }

    public Boolean unlock(Integer uid) {
        return redis.delete(lockKey(uid));
    }

}
