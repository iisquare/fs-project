package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.entity.Role;
import com.iisquare.fs.web.member.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/role")
public class RoleController extends PermitControllerBase {

    @Autowired
    RbacService rbacService;
    @Autowired
    RoleService roleService;
    @Autowired
    MenuService menuService;
    @Autowired
    ResourceService resourceService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        Map<String, Object> result = roleService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/permit")
    @Permission({"", "application", "menu", "resource"})
    public String permitAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        if(id < 1) return ApiUtil.echoResult(1001, "参数异常", id);
        Role info = roleService.info(id);
        if(null == info) return ApiUtil.echoResult(1002, "记录不存在", id);
        Map<String, Object> result = new LinkedHashMap<>();
        String type = DPUtil.parseString(param.get("type"));
        if(!rbacService.hasPermit(request, type)) return ApiUtil.echoResult(9403, null, null);
        int applicationId = DPUtil.parseInt(param.get("applicationId"));
        if(param.containsKey("bids")) {
            if (!roleService.PERMIT_TYPES.containsKey(type)) return ApiUtil.echoResult(1003, "类型异常", id);
            if (!"application".equals(type) && applicationId < 1) return ApiUtil.echoResult(1001, "参数异常", applicationId);
            Set<Integer> bids = new HashSet<>((Collection<Integer>) param.get("bids"));
            bids = roleService.permit(info.getId(), type, bids, applicationId, rbacService.uid(request));
            return ApiUtil.echoResult(0, null, bids);
        } else {
            // 授权树固定覆盖指定应用下的全部节点（不按状态过滤），不受调用方传入的状态条件影响
            Map<Object, Object> treeParam = DPUtil.buildMap("applicationId", applicationId);
            switch (type) {
                case "menu":
                    result.put("tree", menuService.tree(treeParam, DPUtil.buildMap("withStatusText", true)));
                    break;
                case "resource":
                    result.put("tree", resourceService.tree(treeParam, DPUtil.buildMap("withStatusText", true)));
                    break;
                case "application":
                    result.put("tree", new ArrayList<>());
                    break;
                default:
                    return ApiUtil.echoResult(1003, "类型异常", id);
            }
            result.put("checked", roleService.permit(info.getId(), type, null, applicationId, 0));
            return ApiUtil.echoResult(0, null, result);
        }
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = roleService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withApplications", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = roleService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = roleService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", roleService.status());
        model.put("sorts", roleService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

}
