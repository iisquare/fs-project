package com.iisquare.fs.app.flink.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CDCSupport {

    public static String[] patterns(JsonNode options, String key, String defaultValue) {
        JsonNode node = options.get(key);
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) add(result, item.asText(""));
        } else if (node != null && node.isTextual()) {
            for (String item : node.asText().split("[\\r\\n;]+")) add(result, item);
        }
        if (result.isEmpty() && !DPUtil.empty(defaultValue)) result.add(defaultValue);
        return result.toArray(new String[0]);
    }

    public static Properties properties(JsonNode options, String key) {
        Properties properties = new Properties();
        JsonNode node = options.get(key);
        if (node == null || node.isNull()) return properties;
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> properties.put(entry.getKey(), entry.getValue().asText()));
            return properties;
        }
        if (!node.isTextual()) return properties;
        String text = node.asText().trim();
        if (DPUtil.empty(text)) return properties;
        JsonNode json = DPUtil.parseJSON(text);
        if (json != null && json.isObject()) {
            json.fields().forEachRemaining(entry -> properties.put(entry.getKey(), entry.getValue().asText()));
            return properties;
        }
        for (String item : text.split("[&\\r\\n]+")) {
            int split = item.indexOf('=');
            if (split < 1) continue;
            properties.put(item.substring(0, split).trim(), item.substring(split + 1).trim());
        }
        return properties;
    }

    public static Duration duration(JsonNode options, String key, long defaultValue) {
        long value = options.at("/" + key).asLong(0);
        return Duration.ofMillis(value > 0 ? value : defaultValue);
    }

    public static String text(JsonNode options, String key, String defaultValue) {
        String value = options.at("/" + key).asText("");
        return DPUtil.empty(value) ? defaultValue : value;
    }

    private static void add(List<String> result, String value) {
        value = DPUtil.trim(value);
        if (!DPUtil.empty(value) && !result.contains(value)) result.add(value);
    }
}
