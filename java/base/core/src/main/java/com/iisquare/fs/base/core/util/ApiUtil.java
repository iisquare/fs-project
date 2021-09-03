package com.iisquare.fs.base.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApiUtil {

    public static final String FIELD_CODE = "code";
    public static final String FIELD_MSG = "message";
    public static final String FIELD_DATA = "data";

    public static String echoResult(int code, String message, Object data) {
        return echoResult(result(code, message, data));
    }

    public static String echoResult(Map<String, Object> result) {
        return DPUtil.stringify(result);
    }

    public static Map<String, Object> result(int code, String message, Object data) {
        if(null == message) {
            switch (code) {
                case 0:
                    message = "操作成功";
                    break;
                case 403:
                case 9403:
                    message = "禁止访问";
                    break;
                case 404:
                case 9404:
                    message = "信息不存在";
                    break;
                case 500:
                case 9500:
                    message = "操作失败";
                    break;
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(FIELD_CODE, code);
        map.put(FIELD_MSG, message);
        map.put(FIELD_DATA, data);
        return map;
    }

}
