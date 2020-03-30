package com.iisquare.fs.app.crawler.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

public abstract class Output {

    protected final static Logger logger = LoggerFactory.getLogger(Output.class);
    public static final String CONFIG_FILE = "crawler-output.json";
    private static JsonNode config = load();

    public ArrayNode array(JsonNode data) {
        if (data.isArray()) return (ArrayNode) data;
        return DPUtil.arrayNode().add(data);
    }

    public void configure(JsonNode parameters) {

    }

    public void open() throws IOException {

    }

    public void record(JsonNode data) throws Exception {

    }

    public void close() throws IOException {

    }

    private static JsonNode load() {
        String json = FileUtil.getContent(Output.class.getClassLoader().getResource(CONFIG_FILE), false, "UTF-8");
        return DPUtil.parseJSON(json);
    }

    public static JsonNode config(String type) {
        if (null == type) return config;
        return config.at("/" + type + "/property");
    }

    public static JsonNode config(String type, JsonNode data) {
        ObjectNode config = DPUtil.objectNode();
        Iterator<JsonNode> iterator = config(type).elements();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String key = item.get("key").asText();
            if (data.has(key)) {
                config.put(key, data.get(key).asText());
            } else {
                config.put(key, item.get("value").asText());
            }
        }
        config.replace("classname", Output.config.at("/" + type + "/classname"));
        return config;
    }

    public static Output output(String json) {
        if (null == json) return null;
        JsonNode config = DPUtil.parseJSON(json);
        if (null == config) return null;
        return output(config.at("/type").asText(), config.get("property"));
    }

    public static Output output(String type, JsonNode data) {
        if (DPUtil.empty(type)) return null;
        JsonNode config = config(type, data);
        Output output;
        try {
            output = (Output) Class.forName(config.get("classname").asText()).newInstance();
        } catch (Exception e) {
            logger.warn("failed load output from " + config, e);
            return null;
        }
        output.configure(config);
        return output;
    }

}
