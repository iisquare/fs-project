package com.iisquare.fs.web.agent.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.iisquare.fs.base.jpa.config.DruidConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 数据源对应配置 spring.datasource.agent.*，表前缀由 spring.datasource.agent.table-prefix 指定
 */
@Configuration
public class DataSourceConfig {

    @Bean(name = "agentDataSource")
    @Qualifier("agentDataSource")
    @ConfigurationProperties(prefix="spring.datasource.agent")
    @Primary
    public DataSource agentDataSource() throws SQLException {
        return DruidConfiguration.createDataSource();
    }

}
