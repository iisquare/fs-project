package com.iisquare.fs.web.cron.dsconfig;

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

    @Bean(name = "cronDataSource")
    @Qualifier("cronDataSource")
    @ConfigurationProperties(prefix="spring.datasource.cron")
    @Primary
    public DataSource cronDataSource() throws SQLException {
        return DruidConfiguration.createDataSource();
    }

}
