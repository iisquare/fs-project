package com.iisquare.fs.app.crawler.assist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * 协助处理器
 */
public abstract class Assist {

    protected final static Logger logger = LoggerFactory.getLogger(Assist.class);
    public static final String CONFIG_FILE = "crawler-assist.json";
    private static JsonNode config = load();

    public ArrayNode array(JsonNode data) {
        if (data.isArray()) return (ArrayNode) data;
        return DPUtil.arrayNode().add(data);
    }

    public void configure(JsonNode parameters) {

    }

    public boolean open() throws IOException {
        return true;
    }

    public String run() throws Exception {
        return null;
    }

    public void close() throws IOException {

    }

    private static JsonNode load() {
        String json = FileUtil.getContent(Assist.class.getClassLoader().getResource(CONFIG_FILE), false, StandardCharsets.UTF_8);
        return DPUtil.parseJSON(json);
    }

    public static JsonNode config(String type) {
        if (null == type) return config;
        return config.at("/" + type + "/property");
    }

    public static JsonNode config(String type, JsonNode data) {
        if (null == data) data = DPUtil.objectNode();
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
        config.replace("classname", Assist.config.at("/" + type + "/classname"));
        return config;
    }

    public static Assist assist(String json) {
        if (null == json) return null;
        JsonNode config = DPUtil.parseJSON(json);
        if (null == config) return null;
        return assist(config.at("/type").asText(), config.get("property"));
    }

    public static Assist assist(String type, JsonNode data) {
        if (DPUtil.empty(type)) return null;
        JsonNode config = config(type, data);
        Assist assist;
        try {
            assist = (Assist) Class.forName(config.get("classname").asText()).newInstance();
        } catch (Exception e) {
            logger.warn("can not load assist class from " + config);
            return null;
        }
        assist.configure(config);
        return assist;
    }

}
