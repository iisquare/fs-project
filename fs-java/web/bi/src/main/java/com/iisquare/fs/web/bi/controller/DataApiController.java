package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.service.DataApiService;
import com.iisquare.fs.web.bi.service.DatasetService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataApi")
public class DataApiController extends PermitControllerBase {

    @Autowired
    DatasetService datasetService;
    @Autowired
    DataApiService dataApiService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = dataApiService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = dataApiService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = dataApiService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("methods", dataApiService.methods());
        model.put("contentTypes", dataApiService.contentTypes());
        model.put("status", dataApiService.status());
        model.put("fieldTypes", datasetService.fieldTypes());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/test")
    @Permission({"add", "modify"})
    public String testAction(@RequestBody JsonNode config, HttpServletRequest request) {
        Map<String, Object> result = dataApiService.test(config, request);
        return ApiUtil.echoResult(result);
    }

}
