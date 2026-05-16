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

    public static String clientParallel(int clientId, int serverId) {
        return String.format("fs:lm:gateway:parallel-client%d-server%d", clientId, serverId);
    }

    public static String backendParallel(int serverEndpointId) {
        return String.format("fs:lm:gateway:parallel-backend%d", serverEndpointId);
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
