package com.iisquare.fs.web.bi.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.bi.service.OlapService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/olap")
public class OlapController extends PermitControllerBase {

    @Autowired
    OlapService olapService;

    @RequestMapping("/catalogs")
    @Permission("")
    public String catalogsAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = olapService.catalogs(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/schemas")
    @Permission("")
    public String schemasAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = olapService.schemas(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/tables")
    @Permission("")
    public String tablesAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = olapService.tables(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/columns")
    @Permission("")
    public String columnsAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = olapService.columns(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/query")
    @Permission
    public String queryAction(@RequestParam Map<?, ?> param, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = olapService.query(param, false, request, response);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("variables", olapService.variables());
        return ApiUtil.echoResult(0, null, model);
    }
}
