package com.iisquare.fs.web.cron.dsconfig;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import com.iisquare.fs.base.jpa.mvc.PhysicalNamingStrategy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NamingStrategy extends PhysicalNamingStrategy {

    @Value("${spring.datasource.cron.table-prefix}")
    private String tablePrefix;

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        if (name.getText().indexOf("_") > 0) return name;
        name = super.toPhysicalTableName(name, jdbcEnvironment);
        if(null == tablePrefix) return name;
        return getIdentifier(tablePrefix + name.getText(), name.isQuoted(), jdbcEnvironment);
    }
}
