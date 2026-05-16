package com.iisquare.fs.web.flink.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.flink.service.BIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bi")
public class BIController extends PermitControllerBase {

    @Autowired
    private BIService biService;

    @PostMapping("/dag")
    public String dagAction(@RequestBody Map<?, ?> param) {
        JsonNode diagram = DPUtil.toJSON(param);
        return ApiUtil.echoResult(biService.dag(diagram));
    }

    @RequestMapping("/clients")
    public String clientsAction(@RequestParam Map<?, ?> param) {
        return ApiUtil.echoResult(biService.clients());
    }

}
