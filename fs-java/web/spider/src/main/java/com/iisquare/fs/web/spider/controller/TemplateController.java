package com.iisquare.fs.web.spider.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.spider.entity.Template;
import com.iisquare.fs.web.spider.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/template")
public class TemplateController extends PermitControllerBase {

    @Autowired
    private TemplateService templateService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Template info = templateService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = templateService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withTypeText", true, "withRateInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = templateService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = templateService.delete(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", templateService.status());
        model.put("types", templateService.types());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/publish")
    @Permission
    public String publishAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = templateService.publish(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/clear")
    @Permission("publish")
    public String clearAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = templateService.clear(param, request);
        return ApiUtil.echoResult(result);
    }

}
