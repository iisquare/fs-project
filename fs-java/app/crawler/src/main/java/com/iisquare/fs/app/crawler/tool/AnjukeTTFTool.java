package com.iisquare.fs.app.crawler.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.helper.XlabHelper;
import com.iisquare.fs.base.core.util.DPUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Code for JavaScript
 * var Tool = Java.type("com.iisquare.fs.app.crawler.tool.AnjukeTTFTool");
 * var tool = new Tool();
 * tool.loadBase64("...");
 * tool.parse("驋麣麣");
 */
public class AnjukeTTFTool {

    private static WeakHashMap<String, Map<String, Integer>> dicts = new WeakHashMap<>();
    private Map<String, Integer> dict = new HashMap<>();

    public Map<String, Integer> dict() {
        return dict;
    }

    public boolean loadBase64(String base64) {
        Map<String, Integer> dict = dicts.get(base64);
        if (null == dict) {
            ObjectNode data = DPUtil.objectNode().put("type", "anjuke").put("ttf", base64);
            JsonNode json = XlabHelper.ttfExtract(DPUtil.parseString(data));
            if (null == json || 0 != json.get("code").asInt()) return false;
            dict = DPUtil.toJSON(json.get("data"), Map.class);
            if (null == dict) return false;
            dicts.put(base64, dict);
        }
        this.dict = dict;
        return true;
    }

    public String parse(String data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            String item = String.valueOf(data.charAt(i));
            Integer value = dict.get(item);
            sb.append(null == value ? item : value);
        }
        return sb.toString();
    }

}
