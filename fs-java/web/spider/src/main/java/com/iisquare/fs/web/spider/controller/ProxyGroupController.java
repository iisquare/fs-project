package com.iisquare.fs.web.spider.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.spider.entity.ProxyGroup;
import com.iisquare.fs.web.spider.service.ProxyGroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proxy/group")
public class ProxyGroupController extends PermitControllerBase {

    @Autowired
    ProxyGroupService groupService;

    @RequestMapping("/info")
    @Permission("spider:proxyGroup:")
    public String infoAction(@RequestBody Map<?, ?> param) {
        ProxyGroup info = groupService.info(DPUtil.parseInt(param.get("id")));
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("spider:proxyGroup:")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = groupService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withChannelInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/options")
    @Permission("spider:proxyGroup:")
    public String optionsAction() {
        return ApiUtil.echoResult(groupService.options());
    }

    @RequestMapping("/config")
    @Permission("spider:proxyGroup:")
    public String configAction(ModelMap model) {
        model.put("status", groupService.status());
        model.put("strategies", groupService.strategies());
        model.put("sorts", groupService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/save")
    @Permission({"spider:proxyGroup:add", "spider:proxyGroup:modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(groupService.save(param, request));
    }

    @RequestMapping("/delete")
    @Permission("spider:proxyGroup:delete")
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(groupService.remove(ids));
    }

}
