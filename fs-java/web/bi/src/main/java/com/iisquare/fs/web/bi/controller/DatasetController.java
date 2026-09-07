package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.service.DatasetService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataset")
public class DatasetController extends PermitControllerBase {

    @Autowired
    DatasetService datasetService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = datasetService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withRoles", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = datasetService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        Map<String, Object> result = datasetService.remove(ids);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/trigger")
    @Permission({"add", "modify"})
    public String triggerAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = datasetService.trigger(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", datasetService.status());
        model.put("types", datasetService.types());
        model.put("fieldTypes", datasetService.fieldTypes());
        return ApiUtil.echoResult(0, null, model);
    }

}
