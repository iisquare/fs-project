package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.bi.entity.Job;
import com.iisquare.fs.web.bi.service.DAGService;
import com.iisquare.fs.web.bi.service.JobService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/job")
public class JobController extends PermitControllerBase {

    @Autowired
    JobService jobService;
    @Autowired
    DAGService dagService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Job info = jobService.info(id);
        if (null == info) return ApiUtil.echoResult(404, null, info);
        ObjectNode node = (ObjectNode) DPUtil.toJSON(info);
        node.replace("result", DPUtil.parseJSON(node.at("/result").asText()));
        node.replace("outputs", DPUtil.parseJSON(node.at("/outputs").asText()));
        return ApiUtil.echoResult(0, null, node);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = jobService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStateText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(jobService.delete(ids, request));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("states", jobService.states());
        model.put("triggers", jobService.triggers());
        model.put("engines", jobService.engines());
        model.put("models", jobService.models());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/run")
    @Permission("")
    public String runAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("diagramId"), 1, null, 0);
        return ApiUtil.echoResult(dagService.run(id, param, request, Job.TRIGGER_MANUAL));
    }

    @RequestMapping("/cancel")
    @Permission("")
    public String cancelAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        return ApiUtil.echoResult(dagService.cancel(id));
    }

    @RequestMapping("/restart")
    @Permission("")
    public String restartAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        return ApiUtil.echoResult(dagService.restart(id, param, request, Job.TRIGGER_MANUAL));
    }

}
