package com.iisquare.fs.web.lm.core;

import com.iisquare.fs.web.lm.service.GatewayService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisKey {

    public static String rate(String type, int uid, int rateId, long interval) {
        return String.format("fs:lm:gateway:rate-%s-%d-%d-%d", type, uid, rateId, interval);
    }

    public static String reminder(int uid) {
        return String.format("fs:lm:gateway:reminder-%d", uid);
    }

    public static String limited(String type, int uid) {
        return String.format("fs:lm:gateway:limited-%s-%d", type, uid);
    }

    public static String modelParallel(int modelId) {
        return String.format("fs:lm:gateway:model-parallel-%d", modelId);
    }

    public static String channel() {
        return "fs:lm:gateway:channel";
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter gatewayAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(gatewayAdapter, new ChannelTopic(channel()));
        return container;
    }

    @Bean
    public MessageListenerAdapter gatewayAdapter(GatewayService gatewayService) {
        return new MessageListenerAdapter(gatewayService);
    }

}
