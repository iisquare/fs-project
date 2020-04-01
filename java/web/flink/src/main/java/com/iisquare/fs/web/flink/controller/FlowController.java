package com.iisquare.fs.web.flink.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.flink.entity.Flow;
import com.iisquare.fs.web.flink.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/flow")
public class FlowController extends PermitControllerBase {

    @Autowired
    private FlowService flowService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/info")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Flow info = flowService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = flowService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withTypeText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String description = DPUtil.parseString(param.get("description"));
        String content = DPUtil.parseString(param.get("content"));
        Flow info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = flowService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
            if(param.containsKey("name")) {
                if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
                info.setName(name);
            }
            if(param.containsKey("sort")) info.setSort(sort);
            if(param.containsKey("status")) {
                if(!flowService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
                info.setStatus(status);
            }
            if(param.containsKey("description")) info.setDescription(description);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Flow();
            if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
            info.setName(name);
            info.setSort(sort);
            if(!flowService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
            info.setStatus(status);
            info.setDescription(description);
        }
        if(param.containsKey("type")) {
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!flowService.types().containsKey(type)) return ApiUtil.echoResult(1009, "类型异常", type);
            info.setType(type);
        }
        info.setContent(content);
        info = flowService.save(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = flowService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", flowService.status("default"));
        model.put("types", flowService.types());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/plain")
    public String plainAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Flow info = flowService.info(id);
        if(null == info) return "";
        return info.getContent();
    }

}
