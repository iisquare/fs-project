package com.iisquare.fs.base.calcite.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class CalciteSession implements Closeable {

    CalciteConnection connection;
    SchemaPlus schema;

    public CalciteSession() throws Exception {
        Class.forName("org.apache.calcite.jdbc.Driver");
        Properties info = new Properties();
        info.setProperty("lex", "JAVA");
        Connection connection = DriverManager.getConnection("jdbc:calcite:", info);
        this.connection = connection.unwrap(CalciteConnection.class);
        this.schema = this.connection.getRootSchema();
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(connection);
    }

    public SchemaPlus schema() {
        return this.schema;
    }

    public Schema mysql(String name, JsonNode config) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        BasicDataSource datasource = new BasicDataSource();
        datasource.setUrl(config.at("/url").asText());
        datasource.setUsername(config.at("/username").asText());
        datasource.setPassword(config.at("/password").asText());
        String database = config.at("/database").asText();
        Schema schema = JdbcSchema.create(this.schema, name, datasource, database, null);
        this.schema.add(name, schema);
        return schema;
    }

    public ArrayNode query(String sql) throws Exception {
        Statement statement = this.connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        ArrayNode result = result(rs);
        FileUtil.close(rs, statement);
        return result;
    }

    public static ArrayNode result(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        ArrayNode result = DPUtil.arrayNode();
        while (rs.next()) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
                item.put(meta.getColumnName(i), rs.getObject(i));
            }
            result.add(DPUtil.toJSON(item));
        }
        return result;
    }

}
