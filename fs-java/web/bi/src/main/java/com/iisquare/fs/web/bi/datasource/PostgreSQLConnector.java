package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import org.postgresql.Driver;

import java.sql.Connection;

public class PostgreSQLConnector extends JDBCConnector {

    public PostgreSQLConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public Connection open() throws Exception {
        Class.forName(Driver.class.getName());
        return super.open();
    }

}
