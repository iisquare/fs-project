package com.iisquare.fs.web.quartz.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "quartzDataSource")
    @Qualifier("quartzDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.quartz")
    @Primary
    @RefreshScope
    public DataSource quartzDataSource() {
        return DataSourceBuilder.create().build();
    }
}
