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
     * 打包获取当前用户的会话信息（user）与全部已授权资源（resource），供其他服务一次性获取鉴权数据。
     * 返回的 data 对象中仅包含请求参数中声明的键。
     * {
     *     "user": {
     *         "uid": 1
     *     },
     *     "resource": {
     *         "member::": true,
     *         "member:user:list": true,
     *         "member:user:save": true
     *     }
     * }
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
     * 获取当前登录用户的会话信息，data 为会话属性集合。
     * {
     *     "uid": 1
     * }
     */
    @PostMapping("/currentInfo")
    public String currentInfoAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.currentInfo(request));
    }

    /**
     * 根据用户标识、角色标识批量获取用户信息和角色信息，结果分别以ID为键，包含全部状态的记录。
     * 每个用户对象附带其所属的全部角色（roles，键为角色ID）；顶层roles为所请求角色标识对应的角色信息。
     * {
     *     "users": {
     *         "1": {
     *             "id": 1,
     *             "serial": "admin",
     *             "name": "管理员",
     *             "status": 1,
     *             "roles": {
     *                 "1": {"id": 1, "name": "管理员", "status": 1}
     *             }
     *         }
     *     },
     *     "roles": {
     *         "1": {"id": 1, "name": "管理员", "status": 1}
     *     }
     * }
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
     * 获取当前登录用户的个人信息及所属的角色信息，仅返回启用状态的用户和角色，不包含status字段；
     * 用户不存在或status!=1时 data 返回 null。
     * {
     *     "id": 1,
     *     "serial": "admin",
     *     "name": "管理员",
     *     "email": "admin@iisquare.com",
     *     "phone": "",
     *     "roles": {
     *         "1": {"id": 1, "name": "管理员"}
     *     }
     * }
     */
    @PostMapping("/identity")
    public String identityAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        JsonNode identity = rbacService.identity(request);
        return ApiUtil.echoResult(0, null, identity == null || identity.isEmpty() ? null : identity);
    }

    /**
     * 根据用户ID获取指定用户的个人信息及所属的角色信息，返回结构与 /identity 一致。
     * {
     *     "id": 2,
     *     "serial": "admin",
     *     "name": "管理员",
     *     "email": "admin@iisquare.com",
     *     "phone": "",
     *     "roles": {
     *         "2": {"id": 2, "name": "普通用户"}
     *     }
     * }
     */
    @PostMapping("/identityById")
    public String identityByIdAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        JsonNode identity = rbacService.identity(DPUtil.parseInt(param.get("id")));
        return ApiUtil.echoResult(0, null, identity == null || identity.isEmpty() ? null : identity);
    }

    /**
     * 获取当前登录用户已授权的资源信息。
     * 请求体为空时返回全部授权项；传入权限键（格式为 module:controller:action）时仅返回所查询键的授权结果。
     * {
     *     "member::": true,
     *     "member:user:list": true,
     *     "member:user:save": true
     * }
     */
    @PostMapping("/resource")
    public String resourceAction(@RequestBody Map<String, Boolean> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.resource(request, param));
    }

    /**
     * 获取当前登录用户可见的应用及菜单树，data 为应用数组，菜单通过 children 嵌套。
     * [
     *     {
     *         "id": 1,
     *         "name": "系统管理",
     *         "icon": "setting",
     *         "url": "/member",
     *         "target": "_self",
     *         "description": "",
     *         "children": [
     *             {
     *                 "id": 10,
     *                 "parentId": 1,
     *                 "name": "用户管理",
     *                 "icon": "user",
     *                 "url": "/member/user",
     *                 "target": "_self",
     *                 "description": "",
     *                 "children": []
     *             }
     *         ]
     *     }
     * ]
     */
    @PostMapping("/menu")
    public String menuAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.menu(request));
    }

    /**
     * 判断当前登录用户是否具有请求体中所列任一权限，data 为布尔值。
     */
    @PostMapping("/hasPermit")
    public String hasPermitAction(@RequestBody Map<String, Boolean> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.hasPermit(request, param));
    }

    /**
     * 获取或更新配置信息。
     * 请求体不带 alter 时为获取配置，data 为 name -> content 的映射；带 alter 时为批量更新，data 为更新条数。
     * 获取配置： {
     *     "captchaEnabled": "1",
     *     "defaultPassword": "123456"
     * }
     * 更新配置：1
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
     * 记录数据访问日志，并返回当前调用方对相关数据模型拥有的数据权限配置。
     * {
     *     "user": {
     *         "pks": ["id"],
     *         "filters": [{"deleted": 0}],
     *         "fields": [
     *             {"name": "id", "label": "用户标识"},
     *             {"name": "name", "label": "姓名"}
     *         ]
     *     }
     * }
     */
    @PostMapping("/data")
    public String dataAction(@RequestBody JsonNode json, HttpServletRequest request) {
        List<String> permits = DPUtil.toJSON(json.at("/permits"), List.class);
        Map<String, Object> result = dataLogService.record(
                request, json.at("/logParams"), json.at("/params"), permits);
        return ApiUtil.echoResult(result);
    }

}
