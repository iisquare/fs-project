package com.iisquare.fs.web.demo.dsconfig;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import com.iisquare.fs.base.jpa.mvc.PhysicalNamingStrategy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NamingStrategy extends PhysicalNamingStrategy {

    @Value("${spring.datasource.primary.table-prefix}")
    private String tablePrefix;

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return toPhysicalTableName(name, jdbcEnvironment, tablePrefix);
    }
}
