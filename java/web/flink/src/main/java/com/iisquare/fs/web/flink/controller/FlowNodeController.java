package com.iisquare.fs.web.flink.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.flink.entity.FlowNode;
import com.iisquare.fs.web.flink.service.FlowNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flowNode")
public class FlowNodeController extends PermitControllerBase {

    @Autowired
    private FlowNodeService flowNodeService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/tree")
    @Permission("")
    public String treeAction(@RequestParam Map<?, ?> param) {
        return DPUtil.stringify(flowNodeService.tree());
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = flowNodeService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withParentInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!flowNodeService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        int parentId = DPUtil.parseInt(param.get("parentId"));
        if(parentId < 0) {
            return ApiUtil.echoResult(1003, "上级节点异常", name);
        } else if(parentId > 0) {
            FlowNode parent = flowNodeService.info(parentId);
            if(null == parent || !flowNodeService.status("default").containsKey(parent.getStatus())) {
                return ApiUtil.echoResult(1004, "上级节点不存在或已删除", name);
            }
        }
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(DPUtil.empty(type)) return ApiUtil.echoResult(1005, "类型异常", name);
        String plugin = DPUtil.trim(DPUtil.parseString(param.get("plugin")));
        if(DPUtil.empty(plugin)) return ApiUtil.echoResult(1006, "插件名称异常", name);
        FlowNode info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = flowNodeService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new FlowNode();
        }
        info.setName(name);
        info.setParentId(parentId);
        info.setType(type);
        info.setPlugin(plugin);
        info.setIcon(DPUtil.trim(DPUtil.parseString(param.get("icon"))));
        info.setState(DPUtil.trim(DPUtil.parseString(param.get("state"))));
        info.setClassname(DPUtil.trim(DPUtil.parseString(param.get("classname"))));
        info.setDraggable(DPUtil.parseBoolean(param.get("draggable")) ? 1 : 0);
        info.setProperty(DPUtil.trim(DPUtil.parseString(param.get("property"))));
        info.setReturns(DPUtil.trim(DPUtil.parseString(param.get("returns"))));
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        info = flowNodeService.save(info, rbacService.uid(request));
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
        boolean result = flowNodeService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", flowNodeService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
