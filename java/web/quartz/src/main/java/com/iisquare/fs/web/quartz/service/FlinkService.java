package com.iisquare.fs.web.quartz.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Service
@FeignClient(name = "springcloud-web-flink", fallback = FlinkFallback.class)
public interface FlinkService {

    @RequestMapping(method = RequestMethod.GET, value = "/flow/plain")
    String plain(@RequestParam Map<String, Object> params);

    @RequestMapping(method = RequestMethod.POST, value = "/flow/submit")
    String submit(Map<String, Object> params);
}

@Component
class FlinkFallback implements FlinkService {

    @Override
    public String plain(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String submit(Map<String, Object> params) {
        return fallback();
    }

    private String fallback() {
        return ApiUtil.echoResult(500, "调用flink服务失败", null);
    }
}
