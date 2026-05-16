package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.PermitInterceptor;
import com.iisquare.fs.web.core.rbac.RpcControllerBase;
import com.iisquare.fs.web.member.service.DataLogService;
import com.iisquare.fs.web.member.service.RbacService;
import com.iisquare.fs.web.member.service.RoleService;
import com.iisquare.fs.web.member.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rbac")
public class RbacController extends RpcControllerBase {

    @Autowired
    RbacService rbacService;
    @Autowired
    UserService userService;
    @Autowired
    RoleService roleService;
    @Autowired
    DataLogService dataLogService;

    /**
     * 打包获取用户信息和角色资源
     */
    @PostMapping("/pack")
    public String packAction(@RequestBody Map<String, ?> param, HttpServletRequest request) {
        ObjectNode result = DPUtil.objectNode();
        for (Map.Entry<String, ?> entry : param.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case PermitInterceptor.ATTRIBUTE_USER:
                    result.replace(key, rbacService.currentInfo(request));
                    break;
                case PermitInterceptor.ATTRIBUTE_RESOURCE:
                    result.replace(key, rbacService.resource(request));
                    break;
            }
        }
        return ApiUtil.echoResult(0, null, result);
    }

    /**
     * 根据用户标识获取用户信息
     */
    @PostMapping("/listByIds")
    public String listByIdsAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(0, null, userService.infoByIds(ids));
    }

    /**
     * 获取当前登录用户Session信息
     */
    @PostMapping("/currentInfo")
    public String currentInfoAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.currentInfo(request));
    }

    /**
     * 获取当前登录用户资源信息
     */
    @PostMapping("/resource")
    public String resourceAction(@RequestBody Map<String, Boolean> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.resource(request, param));
    }

    /**
     * 获取当前登录用户菜单信息
     */
    @PostMapping("/menu")
    public String menuAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.menu(request));
    }

    /**
     * 判断用户是否具有指定的权限
     */
    @PostMapping("/hasPermit")
    public String hasPermitAction(@RequestBody Map<String, Boolean> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.hasPermit(request, param));
    }

    /**
     * 根据用户标识、角色标识获取用户信息和角色信息
     */
    @PostMapping("/infos")
    public String infosAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        ObjectNode result = DPUtil.objectNode();
        if (param.containsKey("userIds")) {
            result.replace("users", userService.infos(DPUtil.parseIntList(param.get("userIds"))));
        }
        if (param.containsKey("roleIds")) {
            result.replace("roles", roleService.infos(DPUtil.parseIntList(param.get("roleIds"))));
        }
        return ApiUtil.echoResult(0, null, result);
    }

    /**
     * 获取当前登录用户的个人信息及所属的角色信息
     */
    @PostMapping("/identity")
    public String identityAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        JsonNode identity = rbacService.identity(request);
        return ApiUtil.echoResult(0, null, identity);
    }

    /**
     * 获取或更新配置信息
     */
    @PostMapping("/setting")
    public String settingAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String type = DPUtil.parseString(param.get("type"));
        if (DPUtil.empty(param.get("alter"))) {
            List<String> include = (List<String>) param.get("include");
            List<String> exclude = (List<String>) param.get("exclude");
            return ApiUtil.echoResult(0, null, rbacService.setting(type, include, exclude));
        } else {
            Map<String, String> data = (Map<String, String>) param.get("data");
            return ApiUtil.echoResult(0, null, rbacService.setting(type, data));
        }
    }

    /**
     * 获取数据权限配置信息
     */
    @PostMapping("/data")
    public String dataAction(@RequestBody JsonNode json, HttpServletRequest request) {
        List<String> permits = DPUtil.toJSON(json.at("/permits"), List.class);
        Map<String, Object> result = dataLogService.record(
                request, json.at("/logParams"), json.at("/params"), permits);
        return ApiUtil.echoResult(result);
    }

}
