package com.iisquare.fs.base.web.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import feign.Response;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RpcUtil {

    /**
     * 从feign.Response中提取响应体字符串
     */
    public static String string(Response response) {
        if (null == response) return null;
        try {
            return IOUtils.toString(response.body().asInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 从feign.Response中提取响应体并解析为JsonNode
     */
    public static JsonNode json(Response response) {
        String body = string(response);
        if (null == body) return null;
        return DPUtil.parseJSON(body);
    }

    public static JsonNode nullable(boolean nullable) {
        return nullable ? null : DPUtil.objectNode();
    }

    /**
     * 从JSON字符串中解析data字段
     */
    public static JsonNode data(String json, boolean nullable) {
        JsonNode result = DPUtil.parseJSON(json);
        if (null == result) return nullable(nullable);
        if (result.at("/code").asInt() != 0) return nullable(nullable);
        return result.at("/data");
    }

    /**
     * 从feign.Response中解析data字段
     */
    public static JsonNode data(Response response, boolean nullable) {
        JsonNode result = json(response);
        if (null == result) return nullable(nullable);
        if (result.at("/code").asInt() != 0) return nullable(nullable);
        return result.at("/data");
    }

    public static JsonNode data(Map<String, Object> result, boolean nullable) {
        if (ApiUtil.failed(result)) return nullable(nullable);
        return DPUtil.toJSON(ApiUtil.data(result, Object.class));
    }

    /**
     * 从JSON字符串中解析为Map结果
     */
    public static Map<String, Object> result(String json) {
        JsonNode result = DPUtil.parseJSON(json);
        if (null == result) return ApiUtil.result(100403, "解析返回值异常", null);
        int code = result.at("/" + ApiUtil.FIELD_CODE).asInt(100500);
        String message = result.at("/" + ApiUtil.FIELD_MSG).asText();
        Object data = result.at("/" + ApiUtil.FIELD_DATA);
        return ApiUtil.result(code, message, data);
    }

    /**
     * 从feign.Response中解析为Map结果
     */
    public static Map<String, Object> result(Response response) {
        return result(string(response));
    }

}
