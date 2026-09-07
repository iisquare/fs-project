package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.service.DataExcelService;
import com.iisquare.fs.web.bi.service.DatasetService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataExcel")
public class DataExcelController extends PermitControllerBase {

    @Autowired
    DatasetService datasetService;
    @Autowired
    DataExcelService dataExcelService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = dataExcelService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = dataExcelService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = dataExcelService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("modes", dataExcelService.modes());
        model.put("status", dataExcelService.status());
        model.put("fieldTypes", datasetService.fieldTypes());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/template")
    @Permission("")
    public String templateAction(@RequestParam Map<String, Object> param, HttpServletResponse response) {
        Integer id = DPUtil.parseInt(param.get("id"));
        Map<String, Object> result = dataExcelService.template(id, response);
        return ApiUtil.echoResult(result);
    }

    @PostMapping("/upload")
    @Permission("")
    public String uploadAction(@RequestParam Map<String, Object> param, @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = dataExcelService.upload(param, file);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/dataList")
    @Permission("")
    public String dataListAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = dataExcelService.listMongoData(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/dataDelete")
    @Permission("")
    public String dataDeleteAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = dataExcelService.removeMongoData(param);
        return ApiUtil.echoResult(result);
    }

}
