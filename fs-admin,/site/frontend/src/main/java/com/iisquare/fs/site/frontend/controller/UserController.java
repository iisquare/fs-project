package com.iisquare.fs.site.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.site.core.service.UserService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController extends PermitControllerBase {

    @Autowired
    UserService userService;

    @RequestMapping("/info")
    public String infoAction(@RequestBody JsonNode params, HttpServletRequest request) {
        Map<String, Object> result = userService.info(params, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/login")
    public String loginAction(@RequestBody JsonNode params, HttpServletRequest request) {
        Map<String, Object> result = userService.login(params, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/logout")
    public String logoutAction(@RequestBody JsonNode params, HttpServletRequest request) {
        Map<String, Object> result = userService.logout(params, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/signup")
    public String signupAction(@RequestBody JsonNode params, HttpServletRequest request) {
        Map<String, Object> result = userService.signup(params, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/forgot")
    public String forgotAction(@RequestBody JsonNode params, HttpServletRequest request) {
        Map<String, Object> result = userService.forgot(params, request);
        return ApiUtil.echoResult(result);
    }

}
