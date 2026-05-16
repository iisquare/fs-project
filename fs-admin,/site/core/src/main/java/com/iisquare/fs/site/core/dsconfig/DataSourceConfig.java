package com.iisquare.fs.site.core.dsconfig;

import com.iisquare.fs.base.jpa.config.DruidConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class DataSourceConfig {

    @Bean(name = "siteDataSource")
    @Qualifier("siteDataSource")
    @ConfigurationProperties(prefix="spring.datasource.site")
    @Primary
    public DataSource siteDataSource() throws SQLException {
        return DruidConfiguration.createDataSource();
    }

}
