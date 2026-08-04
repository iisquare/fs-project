package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;

public class MySQLConnector extends JDBCConnector {

    public MySQLConnector(JsonNode config) {
        super(config);
    }
}
