package com.iisquare.fs.web.govern.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.govern.service.MetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/meta")
public class MetaController extends PermitControllerBase {

    @Autowired
    private MetaService metaService;

    @GetMapping("/statistic")
    @Permission("model:")
    public String statisticAction(@RequestParam Map<String, Object> param) {
        ObjectNode statistic = metaService.statistic();
        return ApiUtil.echoResult(0, null, statistic);
    }

    @RequestMapping("/search")
    @Permission("model:")
    public String searchAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = metaService.search(param, DPUtil.buildMap("withRowsArray", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/blood")
    @Permission("model:")
    public String bloodAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = metaService.blood(param, DPUtil.buildMap());
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/influence")
    @Permission("model:")
    public String influenceAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = metaService.influence(param, DPUtil.buildMap());
        return ApiUtil.echoResult(result);
    }

}
