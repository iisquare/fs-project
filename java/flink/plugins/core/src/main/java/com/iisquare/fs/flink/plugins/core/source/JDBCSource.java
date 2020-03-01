package com.iisquare.fs.flink.plugins.core.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JDBCSource extends RichSourceFunction<Map<String, Object>> {

    private String config;
    private PreparedStatement ps;
    private Connection connection;

    public JDBCSource(String config) {
        this.config = config;
    }

    @Override
    public void run(SourceContext<Map<String, Object>> ctx) throws Exception {
        ResultSet rs = ps.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        int count = rsmd.getColumnCount();

        while (rs.next()) {
            Map<String, Object> record = new LinkedHashMap<String, Object>();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                record.put(rsmd.getColumnName(i + 1), rs.getObject(rsmd.getColumnName(i + 1)));
            }
            ctx.collect(record);
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        JsonNode config = DPUtil.parseJSON(this.config);
        Class.forName(config.get("driver").asText("com.mysql.jdbc.Driver"));
        connection = DriverManager.getConnection(
            config.get("url").asText(),
            config.get("username").asText(),
            config.get("password").asText()
        );
        ps = connection.prepareStatement(config.get("sql").asText());
    }

    @Override
    public void close() throws Exception {
        super.close();
        if(null != ps) ps.close();
        if(null != connection) connection.close();
    }

    @Override
    public void cancel() {

    }
}
