package com.iisquare.fs.web.spider.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.spider.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/node")
public class NodeController extends PermitControllerBase {

    @Autowired
    private NodeService nodeService;

    @GetMapping("/state")
    public String stateAction(@RequestParam Map<String, String> param) {
        try {
            ObjectNode data = nodeService.state();
            return ApiUtil.echoResult(0, null, data);
        } catch (Exception e) {
            return ApiUtil.echoResult(1500, "获取节点状态异常", e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Permission("")
    public String statsAction(@RequestParam Map<String, String> param) {
        Map<String, Object> result = nodeService.states(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/start")
    @Permission("")
    public String startAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = nodeService.start(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/stop")
    @Permission("")
    public String stopAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = nodeService.stop(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/job")
    @Permission
    public String jobAction(@RequestBody Map<?, ?> param) {
        ObjectNode jobs = nodeService.jobs();
        return ApiUtil.echoResult(0, null, jobs);
    }

    @GetMapping("/ack")
    @Permission("job")
    public String ackAction(@RequestParam Map<String, String> param) {
        Map<String, Object> result = nodeService.ack(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/discardAck")
    @Permission("job")
    public String discardAckAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.discardAck(json);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/execute")
    @Permission("job")
    public String executeAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.execute(json);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/obtain")
    public String obtainAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.obtain(json);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/report")
    public String reportAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.report(json);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/startJob")
    @Permission("job")
    public String startJobAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.startJob(json);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/pauseJob")
    @Permission("job")
    public String pauseJobAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.pauseJob(json);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/stopJob")
    @Permission("job")
    public String stopJobAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.stopJob(json);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/clearJob")
    @Permission("job")
    public String clearJobAction(@RequestBody ObjectNode json) {
        Map<String, Object> result = nodeService.clearJob(json);
        return ApiUtil.echoResult(result);
    }

}
