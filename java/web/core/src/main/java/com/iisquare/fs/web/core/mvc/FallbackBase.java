package com.iisquare.fs.web.core.mvc;

import com.iisquare.fs.base.core.util.ApiUtil;

import java.util.Map;

public abstract class FallbackBase implements RpcBase {

    @Override
    public String get(String uri, Map param) {
        return fallback();
    }

    public String get(String uri, Map param, Throwable cause) {
        return fallback(cause);
    }

    @Override
    public String post(String uri, Map param) {
        return fallback();
    }

    public String post(String uri, Map param, Throwable cause) {
        return fallback(cause);
    }

    public String fallback() {
        String message = getClass().getSimpleName();
        return ApiUtil.echoResult(4501, message, null);
    }

    public String fallback(Throwable cause) {
        String message = getClass().getSimpleName();
        String data = null == cause ? null : cause.getMessage();
        return ApiUtil.echoResult(4502, message, data);
    }

}
