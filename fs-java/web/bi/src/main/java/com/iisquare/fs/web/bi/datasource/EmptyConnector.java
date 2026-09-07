package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;

import java.util.Map;

/**
 * 始终返回空值的默认连接器
 */
public class EmptyConnector extends DatasourceConnector<Object> {

    public EmptyConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public Object open() throws Exception {
        return this;
    }

    @Override
    public void close(Object o) {

    }

    @Override
    public Map<String, Object> test() {
        return ApiUtil.result(0, null, null);
    }

}
