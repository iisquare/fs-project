package com.iisquare.fs.web.member.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "memberDataSource")
    @Qualifier("memberDataSource")
    @ConfigurationProperties(prefix="spring.datasource.member")
    @Primary
    public DataSource memberDataSource() {
        return DataSourceBuilder.create().build();
    }

}
