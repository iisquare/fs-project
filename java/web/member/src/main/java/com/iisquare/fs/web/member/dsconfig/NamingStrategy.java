package com.iisquare.fs.web.member.dsconfig;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NamingStrategy extends SpringPhysicalNamingStrategy {

    @Value("${spring.datasource.member.table-prefix}")
    private String tablePrefix;

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        name = super.toPhysicalTableName(name, jdbcEnvironment);
        if(null == tablePrefix) return name;
        return getIdentifier(tablePrefix + name.getText(), name.isQuoted(), jdbcEnvironment);
    }
}
