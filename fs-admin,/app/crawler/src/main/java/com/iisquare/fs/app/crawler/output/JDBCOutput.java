package com.iisquare.fs.app.crawler.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.app.crawler.helper.SQLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class JDBCOutput extends Output {

    protected final static Logger logger = LoggerFactory.getLogger(JDBCOutput.class);
    private String driver;
    private String url;
    private String username;
    private String password;
    private String table;
    private SQLHelper helper;

    @Override
    public void configure(JsonNode parameters) {
        driver = parameters.at("/driver").asText();
        url = parameters.at("/url").asText();
        username = parameters.at("/username").asText();
        password = parameters.at("/password").asText();
        table = parameters.at("/table").asText();
    }

    @Override
    public void open() throws IOException {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            logger.warn("jdbc driver not found", e);
        }
        helper = new SQLHelper(url, username, password, table);
    }

    @Override
    public void record(JsonNode data) throws Exception {
        Iterator<JsonNode> iterator = array(data).elements();
        while (iterator.hasNext()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = iterator.next().fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                map.put("`" + entry.getKey() + "`", DPUtil.toJSON(entry.getValue(), Object.class));
            }
            helper.insert(map, true);
            if (helper.getLastException() != null) {
                logger.warn("run sql failed " + helper.getLastSql() + ", data:" + DPUtil.stringify(map), helper.getLastException());
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (null != helper) {
            helper.close();
        }
    }
}
