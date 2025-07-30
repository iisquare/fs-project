package com.iisquare.fs.base.jpa.mvc;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * use: spring.jpa.properties.hibernate.naming.physical-strategy
 */
public class PhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    public Identifier toPhysicalTableName(
            Identifier name, JdbcEnvironment jdbcEnvironment, String tablePrefix) {
        name = super.toPhysicalTableName(name, jdbcEnvironment);
        if(null == tablePrefix) return name;
        return getIdentifier(tablePrefix + name.getText(), name.isQuoted(), jdbcEnvironment);
    }

}
