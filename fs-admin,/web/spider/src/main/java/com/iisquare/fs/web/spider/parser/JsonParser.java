package com.iisquare.fs.web.spider.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

public class JsonParser extends Parser {

    public static final String PROTOCOL = "json"; // 标识符

    @Override
    public JsonNode parse(String html, String baseUri) throws Exception {
        return DPUtil.parseJSON(html);
    }

}
