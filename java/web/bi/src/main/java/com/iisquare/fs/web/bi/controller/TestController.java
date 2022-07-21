package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.service.LakeService;
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
    @Autowired
    private LakeService lakeService;

    @GetMapping("/random")
    public String randomAction() {
        List<Double> result = sparkService.random();
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

    @GetMapping("/db2lake")
    public String db2lakeAction(@RequestParam Map<?, ?> param) {
        return ApiUtil.echoResult(lakeService.db2lake(param));
    }

    @PostMapping("/sync2lake")
    public String sync2lakeAction(@RequestBody Map<?, ?> param) throws Exception {
        JsonNode json = DPUtil.toJSON(param);
        return ApiUtil.echoResult(lakeService.sync2lake(json));
    }

    @GetMapping("/kafka2lake")
    public String kafka2lakeAction(@RequestParam Map<?, ?> param) throws Exception {
        return ApiUtil.echoResult(lakeService.kafka2lake(param));
    }

    @GetMapping("/lake2table")
    public String lake2tableAction(@RequestParam Map<?, ?> param) throws Exception {
        return ApiUtil.echoResult(lakeService.lake2table(param));
    }

    @GetMapping("/table2query")
    public String table2queryAction(@RequestParam Map<?, ?> param) throws Exception {
        return ApiUtil.echoResult(lakeService.table2query(param));
    }

}
