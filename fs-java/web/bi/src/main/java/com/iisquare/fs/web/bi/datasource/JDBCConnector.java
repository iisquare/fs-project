package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class JDBCConnector extends DatasourceConnector {

    public JDBCConnector(JsonNode config) {
        super(config);
    }

    @Override
    public String summary() {
        return String.format("%s:%d", config.at("/host").asText(), config.at("/port").asInt());
    }
}
