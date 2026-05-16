package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.bi.entity.Dataset;
import com.iisquare.fs.web.bi.service.DatasetService;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataset")
public class DatasetController extends PermitControllerBase {

    @Autowired
    private DatasetService datasetService;
    @Autowired
    private DefaultRbacService rbacService;

    private Map<?, ?> param;

    @GetMapping("repeat")
    public String repeatAction() {
        return searchAction(this.param);
    }

    @RequestMapping("/search")
    @Permission
    public String searchAction(@RequestBody Map<?, ?> param) {
        this.param = param;
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        JsonNode preview = DPUtil.toJSON(param.get("preview"));
        if (null == preview) {
            Dataset info = datasetService.info(id);
            if (null == info || 1 != info.getStatus()) {
                return ApiUtil.echoResult(1404, "当前数据集暂不可用", id);
            }
            preview = DPUtil.parseJSON(info.getContent());
        }
        JsonNode query = DPUtil.toJSON(param.get("query"));
        Map<String, Object> result = datasetService.search(preview, query);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/columns")
    @Permission("search")
    public String columnsAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Dataset info = datasetService.info(id);
        if (null == info || 1 != info.getStatus()) {
            return ApiUtil.echoResult(1404, "当前数据集暂不可用", id);
        }
        Map<String, Object> result = datasetService.columns(DPUtil.parseJSON(info.getContent()));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Dataset info = datasetService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = datasetService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withSourceInfo", true, "withStatusText", true
        ));
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
        boolean result = datasetService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", datasetService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
