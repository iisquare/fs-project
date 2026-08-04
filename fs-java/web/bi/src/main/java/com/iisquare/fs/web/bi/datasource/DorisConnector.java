package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;

public class DorisConnector extends JDBCConnector {

    public DorisConnector(JsonNode config) {
        super(config);
    }
}
