package com.iisquare.fs.web.cron.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.cron.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/node")
public class NodeController extends PermitControllerBase {

    @Autowired
    private JobService jobService;

    @GetMapping("/state")
    public String stateAction(@RequestParam Map<String, String> param) {
        try {
            ObjectNode data = jobService.state();
            return ApiUtil.echoResult(0, null, data);
        } catch (Exception e) {
            return ApiUtil.echoResult(1500, "获取调度器状态异常", e.getMessage());
        }
    }

    @GetMapping("/stats")
    public String statsAction(@RequestParam Map<String, String> param) {
        ObjectNode data = DPUtil.objectNode();
        ObjectNode nodes = data.putObject("nodes");
        for (String node : jobService.participants()) {
            String url = "http://" + node;
            String content = HttpUtil.get(url + "/node/state", param);
            if (null == content) return ApiUtil.echoResult(5001, "载入节点信息失败", url);
            JsonNode json = DPUtil.parseJSON(content);
            if (null != json) json = json.get("data");
            if (null != json && !json.isNull()) nodes.replace(node, json);
        }
        data.putPOJO("commands", jobService.commands());
        return ApiUtil.echoResult(0, null, data);
    }

    @RequestMapping("/standby")
    public String standbyAction(@RequestParam Map<?, ?> param) {
        String nodeId = DPUtil.parseString(param.get("nodeId"));
        Map<String, Object> result = jobService.standby(nodeId);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/restart")
    public String restartAction(@RequestParam Map<?, ?> param) {
        String nodeId = DPUtil.parseString(param.get("nodeId"));
        boolean modeForce = DPUtil.parseBoolean(param.get("modeForce"));
        Map<String, Object> result = jobService.restart(nodeId, modeForce);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/shutdown")
    public String shutdownAction(@RequestParam Map<?, ?> param) {
        String nodeId = DPUtil.parseString(param.get("nodeId"));
        boolean modeForce = DPUtil.parseBoolean(param.get("modeForce"));
        Map<String, Object> result = jobService.shutdown(nodeId, modeForce);
        return ApiUtil.echoResult(result);
    }

}
