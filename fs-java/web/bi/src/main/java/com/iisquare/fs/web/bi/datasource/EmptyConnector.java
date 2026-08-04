package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 始终返回空值的默认连接器
 */
public class EmptyConnector extends DatasourceConnector {

    public EmptyConnector(JsonNode config) {
        super(config);
    }

}
