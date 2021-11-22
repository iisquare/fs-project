package com.iisquare.fs.web.bi.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.bi.service.SparkService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController extends PermitControllerBase {

    @Autowired
    private SparkService sparkService;

    @GetMapping("/random")
    public String randomAction() {
        List<Double> result = sparkService.random();
        return ApiUtil.echoResult(0, null, result);
    }

}
