package com.iisquare.fs.web.core.mvc;

import com.iisquare.fs.base.core.util.ApiUtil;

import java.util.Map;

public abstract class FallbackBase implements RpcBase {
    @Override
    public String get(String uri, Map param) {
        return fallback();
    }

    @Override
    public String post(String uri, Map param) {
        return fallback();
    }

    protected String fallback() {
        return ApiUtil.echoResult(4500, "调用服务失败", null);
    }

}
