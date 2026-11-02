package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.bi.entity.Dashboard;
import com.iisquare.fs.web.bi.service.DashboardService;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
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
@RequestMapping("/dashboard")
public class DashboardController extends PermitControllerBase {

    @Autowired
    DashboardService dashboardService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/search")
    @Permission
    public String searchAction(@RequestBody Map<?, ?> param) {
        Integer datasetId = DPUtil.parseInt(param.get("datasetId"));
        JsonNode preview = DPUtil.toJSON(param.get("preview"));
        JsonNode levels = DPUtil.toJSON(param.get("levels"));
        return ApiUtil.echoResult(dashboardService.search(datasetId, preview, levels));
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Dashboard info = dashboardService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = dashboardService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(dashboardService.save(param, request));
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(dashboardService.delete(ids, rbacService.uid(request)));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", dashboardService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
