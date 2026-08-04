package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class DatasourceConnector {

    protected JsonNode config;

    public DatasourceConnector(JsonNode config) {
        this.config = config;
    }

    public static DatasourceConnector connector(String type, JsonNode config) {
        return switch (type) {
            case "mysql" -> new MySQLConnector(config);
            case "doris" -> new DorisConnector(config);
            default -> new EmptyConnector(config);
        };
    }

    public String summary() {
        return "";
    }

}
