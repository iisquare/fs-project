package com.iisquare.fs.app.crawler.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class XlabHelper {

    public static String server = "";
    private static Map<String, String> headers = new LinkedHashMap<>();
    static {
        headers.put("Content-Type", "application/json");
    }

    public static JsonNode post(String uri, String data) {
        String content = HttpUtil.post(server + uri, data, headers);
        if (null == content) return null;
        return DPUtil.parseJSON(content);
    }

    public static JsonNode verifySlide(String data) {
        return post("/verify/slide", data);
    }

    public static JsonNode verifyRoad(String data) {
        return post("/verify/road", data);
    }

    public static JsonNode ttfExtract(String data) {
        return post("/ttf/extract", data);
    }

}
