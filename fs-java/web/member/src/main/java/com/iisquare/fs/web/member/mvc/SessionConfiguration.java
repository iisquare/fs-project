package com.iisquare.fs.web.member.mvc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.HttpSessionIdResolver;

@Configuration
@EnableRedisIndexedHttpSession
public class SessionConfiguration {

    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return new SessionIdResolver();
    }

}
