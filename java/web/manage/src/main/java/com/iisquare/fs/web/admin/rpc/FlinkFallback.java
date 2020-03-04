package com.iisquare.fs.web.admin.rpc;

import com.iisquare.fs.base.core.util.ApiUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class FlinkFallback implements FlinkRpc {

    @Override
    public String jdbc(Map<String, Object> params) {
        return fallback();
    }

    private String fallback() {
        return ApiUtil.echoResult(500, "调用flink服务失败", null);
    }
}
