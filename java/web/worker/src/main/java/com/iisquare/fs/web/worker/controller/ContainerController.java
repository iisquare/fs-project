package com.iisquare.fs.web.worker.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.worker.service.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/container")
public class ContainerController extends PermitControllerBase {

    @Autowired
    private ContainerService containerService;

    @RequestMapping("/submit")
    public String submitAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        String handlerName = DPUtil.trim(DPUtil.parseString(param.get("handlerName")));
        int prefetchCount = ValidateUtil.filterInteger(param.get("prefetchCount"), true, 0, 500, 1);
        int consumerCount = ValidateUtil.filterInteger(param.get("consumerCount"), true, 0, null, 1);
        Map<String, Object> result = containerService.submit(queueName, handlerName, prefetchCount, consumerCount);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/create")
    public String createAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        String handlerName = DPUtil.trim(DPUtil.parseString(param.get("handlerName")));
        int prefetchCount = ValidateUtil.filterInteger(param.get("prefetchCount"), true, 0, 500, 1);
        int consumerCount = ValidateUtil.filterInteger(param.get("consumerCount"), true, 0, null, 1);
        Map<String, Object> result = containerService.create(queueName, handlerName, prefetchCount, consumerCount);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/change")
    public String changeAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        int prefetchCount = ValidateUtil.filterInteger(param.get("prefetchCount"), true, 0, 500, 1);
        int consumerCount = ValidateUtil.filterInteger(param.get("prefetchCount"), true, 0, null, 1);
        Map<String, Object> result = containerService.change(queueName, prefetchCount, consumerCount);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/remove")
    public String removeAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        Map<String, Object> result = containerService.remove(queueName);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/start")
    public String startAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        Map<String, Object> result = containerService.start(queueName);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/stop")
    public String stopAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        Map<String, Object> result = containerService.stop(queueName);
        return ApiUtil.echoResult(result);
    }

    @GetMapping("/state")
    public String stateAction(@RequestParam Map<String, String> param) {
        boolean withQueueKey = !DPUtil.empty(param.get("withQueueKey"));
        JsonNode state = containerService.state(withQueueKey);
        return ApiUtil.echoResult(0, null, state);
    }

}
