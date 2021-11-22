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

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
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
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        String collection = DPUtil.trim(DPUtil.parseString(param.get("collection")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
        }
        if(param.containsKey("status")) {
            if(!datasetService.status("default").containsKey(status)) return ApiUtil.echoResult(1004, "状态参数异常", status);
        }
        Dataset info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = datasetService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Dataset();
        }
        if(param.containsKey("collection") || null == info.getId()) info.setCollection(collection);
        if(param.containsKey("name") || null == info.getId()) info.setName(name);
        if(param.containsKey("content") || null == info.getId()) info.setContent(content);
        if(param.containsKey("description") || null == info.getId()) info.setDescription(description);
        if(param.containsKey("sort") || null == info.getId()) info.setSort(sort);
        if(param.containsKey("status") || null == info.getId()) info.setStatus(status);
        info = datasetService.save(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList(param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
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
