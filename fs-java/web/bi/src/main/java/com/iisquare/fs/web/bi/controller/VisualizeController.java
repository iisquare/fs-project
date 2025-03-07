package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.bi.entity.Visualize;
import com.iisquare.fs.web.bi.service.VisualizeService;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/visualize")
public class VisualizeController extends PermitControllerBase {

    @Autowired
    private VisualizeService visualizeService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/search")
    @Permission
    public String searchAction(@RequestBody Map<?, ?> param) {
        Visualize info = null;
        if (param.containsKey("id")) {
            Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
            info = visualizeService.info(id);
            if (null == info || 1 != info.getStatus()) {
                return ApiUtil.echoResult(1404, "当前报表暂不可用", id);
            }
        }
        Integer datasetId = null == info ? DPUtil.parseInt(param.get("datasetId")) : info.getDatasetId();
        JsonNode preview = null == info ? DPUtil.toJSON(param.get("preview")) : DPUtil.parseJSON(info.getContent());
        JsonNode levels = DPUtil.toJSON(param.get("levels"));
        Map<String, Object> result = visualizeService.search(datasetId, preview, levels);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Visualize info = visualizeService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = visualizeService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withDatasetInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = visualizeService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = visualizeService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", visualizeService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
