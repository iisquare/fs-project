package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.AgenticService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 智能体编排接口，权限键为 agent:agentic:*
 *
 * - list/info/config/run/invoke：agent:agentic:
 * - save/publish：agent:agentic:add 或 agent:agentic:modify
 * - delete：agent:agentic:delete
 */
@RestController
@RequestMapping("/agentic")
public class AgenticController extends PermitControllerBase {

    @Autowired
    AgenticService agenticService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = agenticService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        int id = DPUtil.parseInt(param.get("id"));
        ObjectNode info = agenticService.detail(id);
        if (null == info) return ApiUtil.echoResult(404, null, id);
        return ApiUtil.echoResult(0, null, info);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.save(param, request));
    }

    @RequestMapping("/publish")
    @Permission({"add", "modify"})
    public String publishAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.publish(param, request));
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = agenticService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", agenticService.status());
        model.put("sorts", agenticService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/run")
    @Permission("")
    public String runAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.run(param, request));
    }

    @RequestMapping("/invoke")
    @Permission("")
    public String invokeAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.invoke(param, request));
    }

}
