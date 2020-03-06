package com.iisquare.fs.base.web.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

public class RpcUtil {

    public static JsonNode data(String json) {
        JsonNode result = DPUtil.parseJSON(json);
        if (null == result) return DPUtil.objectNode();
        if (result.at("/code").asInt() != 0) return DPUtil.objectNode();
        return result.at("/data");
    }

}
