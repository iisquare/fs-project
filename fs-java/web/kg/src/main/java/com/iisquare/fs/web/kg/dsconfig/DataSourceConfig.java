package com.iisquare.fs.web.kg.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "kgDataSource")
    @Qualifier("kgDataSource")
    @ConfigurationProperties(prefix="spring.datasource.kg")
    @Primary
    public DataSource kgDataSource() {
        return DataSourceBuilder.create().build();
    }

}
