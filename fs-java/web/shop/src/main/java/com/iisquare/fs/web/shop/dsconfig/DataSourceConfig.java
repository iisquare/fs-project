package com.iisquare.fs.web.shop.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "shopDataSource")
    @Qualifier("shopDataSource")
    @ConfigurationProperties(prefix="spring.datasource.shop")
    @Primary
    public DataSource shopDataSource() {
        return DataSourceBuilder.create().build();
    }

}
