package com.iisquare.fs.base.web.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;

import java.util.Map;

public class RpcUtil {

    public static JsonNode nullable(boolean nullable) {
        return nullable ? null : DPUtil.objectNode();
    }

    public static JsonNode data(String json, boolean nullable) {
        JsonNode result = DPUtil.parseJSON(json);
        if (null == result) return nullable(nullable);
        if (result.at("/code").asInt() != 0) return nullable(nullable);
        return result.at("/data");
    }

    public static JsonNode data(Map<String, Object> result, boolean nullable) {
        if (ApiUtil.failed(result)) return nullable(nullable);
        return DPUtil.toJSON(ApiUtil.data(result, Object.class));
    }

    public static Map<String, Object> result(String json) {
        JsonNode result = DPUtil.parseJSON(json);
        if (null == result) return ApiUtil.result(100403, "解析返回值异常", null);
        int code = result.at("/" + ApiUtil.FIELD_CODE).asInt(100500);
        String message = result.at("/" + ApiUtil.FIELD_MSG).asText();
        Object data = result.at("/" + ApiUtil.FIELD_DATA);
        return ApiUtil.result(code, message, data);
    }

}
