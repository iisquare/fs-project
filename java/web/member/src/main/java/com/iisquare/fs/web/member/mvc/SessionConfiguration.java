package com.iisquare.fs.web.member.mvc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration;

import javax.annotation.PostConstruct;

@Configuration
public class SessionConfiguration extends RedisHttpSessionConfiguration {

    @Value("${server.servlet.session.cookie.max-age}")
    private int sessionTimeout;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        super.setMaxInactiveIntervalInSeconds(sessionTimeout);
        // fixed: JedisDataException: ERR Unsupported CONFIG parameter: notify-keyspace-events
        super.setConfigureRedisAction(ConfigureRedisAction.NO_OP);
    }

}
