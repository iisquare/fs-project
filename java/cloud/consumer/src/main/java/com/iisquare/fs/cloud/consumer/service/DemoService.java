package com.iisquare.fs.cloud.consumer.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@FeignClient(name = "springcloud-demo-provider",fallback = DemoFallback.class)
public interface DemoService {

    @RequestMapping(method = RequestMethod.POST, value = "/demo/echo")
    String echo(Map<String, Object> param);

}
