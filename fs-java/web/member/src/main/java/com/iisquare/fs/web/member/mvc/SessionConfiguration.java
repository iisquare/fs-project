package com.iisquare.fs.web.member.mvc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.HttpSessionIdResolver;

import java.time.Duration;

@Configuration
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 2592000) // 与 server.servlet.session.timeout 保持一致（30天）
public class SessionConfiguration {

    @Value("${server.servlet.session.cookie.max-age}")
    private Duration maxAge;

    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return new SessionIdResolver(maxAge);
    }

}
