package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.admin.entity.AnalysisNode;
import com.iisquare.fs.web.admin.mvc.Permission;
import com.iisquare.fs.web.admin.mvc.PermitController;
import com.iisquare.fs.web.admin.service.AnalysisNodeService;
import com.iisquare.fs.web.admin.rpc.FlinkRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/analysisNode")
public class AnalysisNodeController extends PermitController {

    @Autowired
    private AnalysisNodeService analysisNodeService;
    @Autowired
    private FlinkRpc flinkRpc;

    @RequestMapping("/draw")
    @Permission("modify")
    public String drawAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        AnalysisNode info = analysisNodeService.info(id);
        model.put("info", info);
        model.put("saveUrl", "/analysisNode/drawSave");
        return displayTemplate(model, request, "flow", "draw");
    }

    @RequestMapping("/drawSave")
    @Permission("modify")
    @ResponseBody
    public String drawSaveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        AnalysisNode info = analysisNodeService.info(id);
        if(null == info) return ApiUtil.echoResult(404, null, id);
        info.setContent(DPUtil.trim(DPUtil.parseString(param.get("content"))));
        info.setProperty(DPUtil.stringify(analysisNodeService.property(info.getContent())));
        info = analysisNodeService.save(info, uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/tree")
    @Permission("")
    @ResponseBody
    public String treeAction(@RequestParam Map<?, ?> param) {
        return DPUtil.stringify(analysisNodeService.tree());
    }

    @RequestMapping("/list")
    @Permission("")
    @ResponseBody
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = analysisNodeService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withParentInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/jdbc")
    @Permission({"add", "modify"})
    @ResponseBody
    public String jdbcAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return flinkRpc.jdbc(param);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    @ResponseBody
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!analysisNodeService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        int parentId = DPUtil.parseInt(param.get("parentId"));
        if(parentId < 0) {
            return ApiUtil.echoResult(1003, "上级节点异常", name);
        } else if(parentId > 0) {
            AnalysisNode parent = analysisNodeService.info(parentId);
            if(null == parent || !analysisNodeService.status("default").containsKey(parent.getStatus())) {
                return ApiUtil.echoResult(1004, "上级节点不存在或已删除", name);
            }
        }
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(DPUtil.empty(type)) return ApiUtil.echoResult(1005, "类型异常", name);
        AnalysisNode info = null;
        if(id > 0) {
            if(!hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = analysisNodeService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new AnalysisNode();
        }
        info.setName(name);
        info.setParentId(parentId);
        info.setType(type);
        info.setIcon(DPUtil.trim(DPUtil.parseString(param.get("icon"))));
        info.setState(DPUtil.trim(DPUtil.parseString(param.get("state"))));
        info.setDraggable(DPUtil.parseBoolean(param.get("draggable")) ? 1 : 0);
        info.setContent(DPUtil.trim(DPUtil.parseString(param.get("content"))));
        info.setProperty(DPUtil.trim(DPUtil.parseString(param.get("property"))));
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        info = analysisNodeService.save(info, uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    @ResponseBody
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = analysisNodeService.delete(ids, uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    @ResponseBody
    public String configAction(ModelMap model) {
        model.put("status", analysisNodeService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
