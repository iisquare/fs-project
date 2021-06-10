package com.iisquare.fs.web.oa.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "oaDataSource")
    @Qualifier("oaDataSource")
    @ConfigurationProperties(prefix="spring.datasource.oa")
    @Primary
    public DataSource oaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "workflowDataSource")
    @Qualifier("workflowDataSource")
    @ConfigurationProperties(prefix="spring.datasource.workflow")
    public DataSource workflowDataSource() {
        return DataSourceBuilder.create().build();
    }

}
