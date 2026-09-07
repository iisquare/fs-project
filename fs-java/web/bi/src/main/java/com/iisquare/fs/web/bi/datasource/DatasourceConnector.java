package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;

import java.util.Map;

public abstract class DatasourceConnector<T> {

    protected String type;
    protected JsonNode config;

    public DatasourceConnector(String type, JsonNode config) {
        this.type = type;
        this.config = config;
    }

    public static DatasourceConnector connector(String type, JsonNode config) {
        return switch (type) {
            case "mysql" -> new MySQLConnector(type, config);
            case "doris" -> new DorisConnector(type, config);
            case "postgresql", "postgres" -> new PostgreSQLConnector(type, config);
            case "http" -> new HttpConnector(type, config);
            case "elasticsearch" -> new ElasticsearchConnector(type, config);
            case "mongodb" -> new MongoDBConnector(type, config);
            default -> new EmptyConnector(type, config);
        };
    }

    public String summary() {
        return "";
    }

    public abstract T open() throws Exception;

    public abstract void close(T t);

    public Map<String, Object> test() {
        try {
            close(open());
        } catch (Exception e) {
            return ApiUtil.result(1500, "连接失败", e.getMessage());
        }
        return ApiUtil.result(0, "连接成功", null);
    }

    /**
     * 将数据源自身的超时配置写入目标配置，未配置时保持缺省。
     */
    protected void timeout(ObjectNode config) {
        if (this.config.has("connectTimeout")) {
            config.put("connectTimeout", this.config.at("/connectTimeout").asInt());
        }
        if (this.config.has("readTimeout")) {
            config.put("readTimeout", this.config.at("/readTimeout").asInt());
        } else if (this.config.has("socketTimeout")) {
            config.put("socketTimeout", this.config.at("/socketTimeout").asInt());
        }
    }

}
