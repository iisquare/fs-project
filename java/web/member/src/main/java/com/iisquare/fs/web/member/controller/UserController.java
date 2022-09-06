package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.entity.User;
import com.iisquare.fs.web.member.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/user")
public class UserController extends PermitControllerBase {

    @Autowired
    private RbacService rbacService;
    @Autowired
    private UserService userService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private RoleService roleService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        ObjectNode result = userService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withRoles", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = userService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = userService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", userService.status("full"));
        model.put("defaultPassword", settingService.get("member", "defaultPassword"));
        Map<?, ?> searchResult = roleService.search(new LinkedHashMap<>(), DPUtil.buildMap("withStatusText", true));
        model.put("roles", searchResult.get("rows"));
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/tree")
    @Permission({"", "role"})
    public String treeAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        if(id < 1) return ApiUtil.echoResult(1001, "参数异常", id);
        User info = userService.info(id);
        if(null == info || -1 == info.getStatus()) return ApiUtil.echoResult(1002, "记录不存在或已删除", id);
        Map<String, Object> result = new LinkedHashMap<>();
        String type = DPUtil.parseString(param.get("type"));
        if(param.containsKey("bids")) {
            switch (type) {
                case "role":
                    if(!rbacService.hasPermit(request, type)) return ApiUtil.echoResult(9403, null, null);
                    Set<Integer> bids = new HashSet<>();
                    bids.addAll((List<Integer>) param.get("bids"));
                    bids = relationService.relationIds("user_" + type, id, bids);
                    return ApiUtil.echoResult(null == bids ? 500 : 0, null, bids);
                default:
                    return ApiUtil.echoResult(1003, "类型异常", id);
            }
        } else {
            result.put("checked", relationService.relationIds("user_" + type, info.getId(), null));
            return ApiUtil.echoResult(0, null, result);
        }
    }

    @RequestMapping("/password")
    public String passwordAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = userService.password(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/login")
    public String loginAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = userService.login(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/logout")
    public String logoutAction(HttpServletRequest request) {
        Map<String, Object> result = userService.logout(request);
        return ApiUtil.echoResult(result);
    }

}
