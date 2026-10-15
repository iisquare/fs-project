package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.ProjectVersionService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分享项目的版本记录（管理端）：按项目查看每个版本的版本号、版本说明与配置原文。
 *
 * 权限沿用分享项目的资源（`agent:projectShare:`），与技能 / 技能版本的组织方式一致：
 * 版本是项目的一部分，不单列一棵资源树。
 */
@RestController
@RequestMapping("/projectVersion")
public class ProjectVersionController extends PermitControllerBase {

    @Autowired
    ProjectVersionService versionService;

    /** 版本详情：带项目配置原文，供管理端查看 */
    @RequestMapping("/info")
    @Permission("projectShare:")
    public String infoAction(@RequestParam Map<?, ?> param) {
        return ApiUtil.echoResult(versionService.info(param));
    }

    @RequestMapping("/list")
    @Permission("projectShare:")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = versionService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withProjectInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission("projectShare:modify")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(versionService.save(param, request));
    }

    @RequestMapping("/delete")
    @Permission("projectShare:delete")
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(versionService.remove(ids));
    }

    @RequestMapping("/config")
    @Permission("projectShare:")
    public String configAction(ModelMap model) {
        model.put("status", versionService.status());
        model.put("sorts", versionService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

}
