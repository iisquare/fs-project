package com.iisquare.fs.app.crawler.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

public class JsonParser extends Parser {

    public static final String PROTOCOL = "json"; // 标识符

    @Override
    public JsonNode parse(String data) throws Exception {
        return DPUtil.parseJSON(data);
    }

}
