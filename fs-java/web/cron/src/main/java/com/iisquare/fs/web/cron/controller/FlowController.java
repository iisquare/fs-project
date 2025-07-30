package com.iisquare.fs.web.cron.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.cron.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/flow")
public class FlowController extends PermitControllerBase {

    @Autowired
    private FlowService flowService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = flowService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        try {
            ObjectNode result = flowService.search(param, DPUtil.buildMap(
                    "withUserInfo", true, "withStatusText", true, "withTriggerInfo", true
            ));
            return ApiUtil.echoResult(0, null, result);
        } catch (Exception e) {
            return ApiUtil.echoResult(5000, "获取查询结果失败", e.getMessage());
        }
    }

    @RequestMapping("/save")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = flowService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = flowService.delete(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", flowService.status());
        return ApiUtil.echoResult(0, null, model);
    }

}
