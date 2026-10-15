package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.ProjectShareService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分享的项目（管理端）：查看与管理所有用户分享上来的项目。
 *
 * 客户端发布与浏览的接口挂在 BotController（权限键 `agent:bot:`）下，两边共用同一套服务；
 * 版本记录见 {@link ProjectVersionController}，沿用本控制器的权限前缀（`agent:projectShare:`）。
 */
@RestController
@RequestMapping("/projectShare")
public class ProjectShareController extends PermitControllerBase {

    @Autowired
    ProjectShareService shareService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        return ApiUtil.echoResult(shareService.info(param));
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = shareService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withRoles", true,
                "withVersionCount", true, "withLatest", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(shareService.save(param, request));
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(shareService.remove(ids));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", shareService.status());
        model.put("sorts", shareService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

}
