package com.iisquare.fs.web.bi.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.bi.service.SparkService;
import com.iisquare.fs.web.bi.service.TestService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController extends PermitControllerBase {

    @Autowired
    private SparkService sparkService;
    @Autowired
    private TestService testService;

    @GetMapping("/random")
    public String randomAction() {
        List<Double> result = sparkService.random();
        return ApiUtil.echoResult(0, null, result);
    }

    @PostMapping("/mongoPushDown")
    public String mongoPushDownAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(testService.mongoPushDown(param));
    }

}
