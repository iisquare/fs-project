package com.iisquare.fs.web.lm.core;

import com.iisquare.fs.web.lm.service.ProxyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisKey {

    public static String clientParallel(int clientId, int serverId) {
        return String.format("fs:lm:proxy:parallel-client%d-server%d", clientId, serverId);
    }

    public static String backendParallel(int serverEndpointId) {
        return String.format("fs:lm:proxy:parallel-backend%d", serverEndpointId);
    }

    public static String channel() {
        return "fs:lm:proxy:channel";
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter proxyAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(proxyAdapter, new ChannelTopic(channel()));
        return container;
    }

    @Bean
    public MessageListenerAdapter proxyAdapter(ProxyService proxyService) {
        return new MessageListenerAdapter(proxyService);
    }

}
