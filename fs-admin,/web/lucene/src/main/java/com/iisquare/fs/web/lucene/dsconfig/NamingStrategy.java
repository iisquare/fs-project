package com.iisquare.fs.web.lucene.dsconfig;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import com.iisquare.fs.base.jpa.mvc.PhysicalNamingStrategy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NamingStrategy extends PhysicalNamingStrategy {

    @Value("${spring.datasource.lucene.table-prefix}")
    private String tablePrefix;

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return toPhysicalTableName(name, jdbcEnvironment, tablePrefix);
    }
}
