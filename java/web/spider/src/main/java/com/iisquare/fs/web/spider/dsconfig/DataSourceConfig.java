package com.iisquare.fs.web.spider.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "spiderDataSource")
    @Qualifier("spiderDataSource")
    @ConfigurationProperties(prefix="spring.datasource.spider")
    @Primary
    public DataSource spiderDataSource() {
        return DataSourceBuilder.create().build();
    }

}
