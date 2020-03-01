package com.iisquare.fs.cloud.consumer.controller;

import com.iisquare.fs.cloud.consumer.service.DemoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Api(description = "内部调用")
@RequestMapping("/feign")
@RestController
public class FeignController {

    @Autowired
    private DemoService demoService;

    @ApiOperation(value = "回显请求参数", httpMethod = "GET", produces = "application/json")
    @ApiResponse(code = 0, message = "success", response = String.class)
    @GetMapping("/echo")
    public String echoAction() {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("time", System.currentTimeMillis());
        String result = demoService.echo(param);
        return result;
    }

}
