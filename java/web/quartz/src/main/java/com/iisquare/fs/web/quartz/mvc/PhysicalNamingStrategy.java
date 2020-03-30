package com.iisquare.fs.web.quartz.mvc;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;

/**
 * use: spring.jpa.properties.hibernate.naming.physical-strategy
 */
public class PhysicalNamingStrategy extends SpringPhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return Identifier.toIdentifier(name.getText().toUpperCase());
    }
}
