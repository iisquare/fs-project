package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.agent.service.SkillService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skill")
public class SkillController extends PermitControllerBase {

    @Autowired
    SkillService skillService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        Map<String, Object> result = skillService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = skillService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withTypeText", true, "withRoles", true));
        return ApiUtil.echoResult(0, null, result);
    }

    /**
     * 客户端可用技能：已启用且授权角色命中当前用户，不含配置内容（content）
     */
    @RequestMapping("/available")
    @Permission("")
    public String availableAction(HttpServletRequest request) {
        return ApiUtil.echoResult(skillService.available(request));
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = skillService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(skillService.remove(ids));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("types", skillService.types());
        model.put("status", skillService.status());
        model.put("sorts", skillService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

}
