package com.iisquare.fs.web.oa.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.iisquare.fs.base.jpa.config.DruidConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class DataSourceConfig {

    @Bean(name = "oaDataSource")
    @Qualifier("oaDataSource")
    @ConfigurationProperties(prefix="spring.datasource.oa")
    @Primary
    public DataSource oaDataSource() throws SQLException {
        return DruidConfiguration.createDataSource();
    }

    @Bean(name = "workflowDataSource")
    @Qualifier("workflowDataSource")
    @ConfigurationProperties(prefix="spring.datasource.workflow")
    public DataSource workflowDataSource() throws SQLException {
        return DruidConfiguration.createDataSource();
    }

}
