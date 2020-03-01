package com.iisquare.fs.web.admin.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Service
@FeignClient(name = "springcloud-web-flink", fallback = FlinkFallback.class)
public interface FlinkRpc {

    @RequestMapping(method = RequestMethod.POST, value = "/flow/jdbc")
    String jdbc(Map<String, Object> params);

}


