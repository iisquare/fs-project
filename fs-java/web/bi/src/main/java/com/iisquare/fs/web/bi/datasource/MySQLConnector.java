package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Connection;

public class MySQLConnector extends JDBCConnector {

    public MySQLConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public Connection open() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return super.open();
    }
}
