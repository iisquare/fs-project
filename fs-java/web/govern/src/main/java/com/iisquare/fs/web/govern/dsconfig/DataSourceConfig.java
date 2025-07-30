package com.iisquare.fs.web.govern.dsconfig;

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

    @Bean(name = "governDataSource")
    @Qualifier("governDataSource")
    @ConfigurationProperties(prefix="spring.datasource.govern")
    @Primary
    public DataSource governDataSource() throws SQLException {
        return DruidConfiguration.createDataSource();
    }

}
