package com.iisquare.fs.cloud.consumer.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DemoFallback implements DemoService {
    @Override
    public String echo(Map<String, Object> param) {
        return ApiUtil.echoResult(500, "Fallback", param);
    }
}
