package com.iisquare.fs.web.bi.dsconfig;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.config.DruidConfiguration;
import io.trino.jdbc.TrinoDriver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
public class DataSourceConfig {

    @Bean(name = "biDataSource")
    @Qualifier("biDataSource")
    @ConfigurationProperties(prefix="spring.datasource.bi")
    @Primary
    public DataSource biDataSource() throws SQLException {
        return DruidConfiguration.createDataSource();
    }

    @Bean(name = "trinoDataSource")
    @Qualifier("trinoDataSource")
    public DataSource trinoDataSource(
            @Value("${fs.bi.trino.url}") String url,
            @Value("${fs.bi.trino.user}") String username,
            @Value("${fs.bi.trino.password}") String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(TrinoDriver.class.getName());
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        if (!DPUtil.empty(username)) {
            Properties properties = new Properties();
            properties.setProperty("SSL", "true");
            properties.setProperty("SSLVerification", "NONE");
            dataSource.setConnectionProperties(properties);
        }
        return dataSource;
    }

}
