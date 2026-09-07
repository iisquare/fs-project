package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置信息： {
 *     "host": "主机地址",
 *     "port": "端口",
 *     "username": "用户名",
 *     "password": "密码",
 *     "database": "数据库名可为空；Postgres建议填写，不指定时默认连接用户同名数据库。",
 *     "paramsQueryString": "附加参数"
 * }
 */
public abstract class JDBCConnector extends DatasourceConnector<Connection> {

    public JDBCConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public String summary() {
        return String.format("%s:%d", config.at("/host").asText(), config.at("/port").asInt());
    }

    @Override
    public Connection open() throws Exception {
        return open(config.at("/database").asText());
    }

    public Connection open(String database) throws Exception {
        String host = config.at("/host").asText();
        int port = config.at("/port").asInt();
        String username = config.at("/username").asText();
        String password = config.at("/password").asText();
        String paramsQueryString = config.at("/paramsQueryString").asText();
        String url = "jdbc:" + protocol() + "://" + host;
        if (port > 0) url += ":" + port;
        url += "/" + DPUtil.parseString(database);
        if (!DPUtil.empty(paramsQueryString)) url += "?" + paramsQueryString;
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public void close(Connection connection) {
        FileUtil.close(connection);
    }

    public String protocol() {
        return type;
    }

    public String url(String database) {
        String host = config.at("/host").asText();
        int port = config.at("/port").asInt();
        String paramsQueryString = config.at("/paramsQueryString").asText();
        String url = "jdbc:" + protocol() + "://" + host;
        if (port > 0) url += ":" + port;
        url += "/" + DPUtil.parseString(database);
        if (!DPUtil.empty(paramsQueryString)) url += "?" + paramsQueryString;
        return url;
    }

    public String baseUrl() {
        String host = config.at("/host").asText();
        int port = config.at("/port").asInt();
        String paramsQueryString = config.at("/paramsQueryString").asText();
        String url = "jdbc:" + protocol() + "://" + host;
        if (port > 0) url += ":" + port;
        if (!DPUtil.empty(paramsQueryString)) url += "?" + paramsQueryString;
        return url;
    }

    public List<String> databases() throws Exception {
        List<String> databases = new ArrayList<>();
        try (Connection connection = open()) {
            try (ResultSet resultSet = connection.getMetaData().getCatalogs()) {
                while (resultSet.next()) {
                    String database = resultSet.getString("TABLE_CAT");
                    if (!DPUtil.empty(database)) {
                        databases.add(database);
                    }
                }
            }
        }
        return databases;
    }

}
