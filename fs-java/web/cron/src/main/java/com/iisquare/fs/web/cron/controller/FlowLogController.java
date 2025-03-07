package com.iisquare.fs.web.cron.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.cron.service.FlowLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/flowLog")
public class FlowLogController extends PermitControllerBase {

    @Autowired
    private FlowLogService logService;

    @RequestMapping("/tick")
    @Permission("flow:")
    public String tickAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = logService.tick(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("flow:")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = logService.search(param, DPUtil.buildMap());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/stages")
    @Permission("flow:")
    public String stagesAction(@RequestBody Map<?, ?> param) {
        int logId = DPUtil.parseInt(param.get("logId"));
        Map<String, Object> result = logService.stages(logId);
        return ApiUtil.echoResult(result);
    }

    @GetMapping("/state")
    public String stateAction() {
        ObjectNode state = logService.state();
        return ApiUtil.echoResult(0, null, state);
    }

    @RequestMapping("/submit")
    public String submitAction(@RequestBody Map<?, ?> param) {
        JsonNode json = DPUtil.toJSON(param);
        Map<String, Object> result = logService.submit(json.at("/stage"), (ObjectNode) json.at("/config"));
        return ApiUtil.echoResult(result);
    }

}
