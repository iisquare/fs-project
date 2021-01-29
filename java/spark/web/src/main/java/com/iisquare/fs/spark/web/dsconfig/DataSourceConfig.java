package com.iisquare.fs.spark.web.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "sparkDataSource")
    @Qualifier("sparkDataSource")
    @ConfigurationProperties(prefix="spring.datasource.primary")
    @Primary
    public DataSource sparkDataSource() {
        return DataSourceBuilder.create().build();
    }

}
