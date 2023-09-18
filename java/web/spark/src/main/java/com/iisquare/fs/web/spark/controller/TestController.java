package com.iisquare.fs.web.spark.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.spark.service.BIService;
import com.iisquare.fs.web.spark.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController extends PermitControllerBase {

    @Autowired
    private BIService biService;
    @Autowired
    private TestService testService;

    @GetMapping("/random")
    public String randomAction() {
        List<Double> result = biService.random();
        return ApiUtil.echoResult(0, null, result);
    }

    @PostMapping("/mongoPushDown")
    public String mongoPushDownAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(testService.mongoPushDown(param));
    }

    @PostMapping("/mysqlPushDown")
    public String mysqlPushDownAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(testService.mysqlPushDown(param));
    }

    @PostMapping("/pushDown")
    public String pushDownAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(testService.pushDown(param));
    }

}
