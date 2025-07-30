package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.service.RbacService;
import com.iisquare.fs.web.member.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController extends PermitControllerBase {

    @Autowired
    private UserService userService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
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
        boolean result = userService.delete(ids, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", userService.status());
        return ApiUtil.echoResult(0, null, model);
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
