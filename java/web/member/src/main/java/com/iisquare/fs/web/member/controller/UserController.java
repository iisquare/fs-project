package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.member.entity.User;
import com.iisquare.fs.web.member.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
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
    private Configuration configuration;
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
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
        if(userService.existsByName("name", id)) return ApiUtil.echoResult(2001, "名称已存在", name);
        String password = DPUtil.trim(DPUtil.parseString(param.get("password")));
        User info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = userService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new User();
            String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
            if(DPUtil.empty(serial)) return ApiUtil.echoResult(1002, "账号不能为空", serial);
            if(userService.existsBySerial(serial)) return ApiUtil.echoResult(2002, "账号已存在", serial);
            info.setSerial(serial);
            if(DPUtil.empty(password)) return ApiUtil.echoResult(1003, "密码不能为空", password);
            info.setCreatedIp(ServletUtil.getRemoteAddr(request));
        }
        if(!DPUtil.empty(password)) {
            String salt = DPUtil.random(4);
            password = userService.password(password, salt);
            info.setPassword(password);
            info.setSalt(salt);
        }
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!userService.status("default").containsKey(status)) {
            return ApiUtil.echoResult(1002, "状态异常", status);
        }
        String description = DPUtil.parseString(param.get("description"));
        info.setName(name);
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        if(param.containsKey("lockedTime")) {
            String lockedTime =  DPUtil.trim(DPUtil.parseString(param.get("lockedTime")));
            if(DPUtil.empty(lockedTime)) {
                info.setLockedTime(0L);
            } else {
                info.setLockedTime(DPUtil.dateTimeToMillis(lockedTime, configuration.getFormatDate()));
            }
        }
        info = userService.save(info, rbacService.uid(request));
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
        boolean result = userService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", userService.status("full"));
        model.put("defaultPassword", settingService.get("member", "default-password"));
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
        String password = DPUtil.trim(DPUtil.parseString(param.get("password")));
        String passwordNew = DPUtil.trim(DPUtil.parseString(param.get("passwordNew")));
        String passwordOld = DPUtil.trim(DPUtil.parseString(param.get("passwordOld")));
        if(DPUtil.empty(passwordOld)) return ApiUtil.echoResult(1001, "请输入原密码", null);
        if(DPUtil.empty(password)) return ApiUtil.echoResult(1002, "请输入新密码", null);
        if(!password.equals(passwordNew)) return ApiUtil.echoResult(1003, "两次密码输入不一致", null);
        User info = userService.info(rbacService.uid(request));
        if(null == info) return ApiUtil.echoResult(1004, "用户未登录或登录超时", null);
        if(!info.getPassword().equals(userService.password(passwordOld, info.getSalt()))) {
            return ApiUtil.echoResult(1005, "原密码错误", null);
        }
        String salt = DPUtil.random(4);
        password = userService.password(password, salt);
        info.setPassword(password);
        info.setSalt(salt);
        userService.save(info, 0);
        logoutAction(request); // 退出登录
        return ApiUtil.echoResult(0, null, null);
    }

    @RequestMapping("/login")
    public String loginAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        User info = null;
        Map<String, Object> session = null;
        String module = DPUtil.parseString(param.get("module"));
        if(param.containsKey("serial")) {
            info = userService.infoBySerial(DPUtil.parseString(param.get("serial")));
            if(null == info) return ApiUtil.echoResult(1001, "账号不存在", null);
            if(!info.getPassword().equals(userService.password(DPUtil.parseString(param.get("password")), info.getSalt()))) {
                return ApiUtil.echoResult(1002, "密码错误", null);
            }
            if(1 != info.getStatus() || info.getLockedTime() > System.currentTimeMillis()) {
                return ApiUtil.echoResult(1003, "账号已锁定，请联系管理人员", null);
            }
            info.setLoginedTime(System.currentTimeMillis());
            info.setLoginedIp(ServletUtil.getRemoteAddr(request));
            userService.save(info, 0);
            session = rbacService.currentInfo(request, DPUtil.buildMap("uid", info.getId()));
            if(!rbacService.hasPermit(request, module, null, null)) {
                logoutAction(request);
                return ApiUtil.echoResult(403, null, null);
            }
        } else {
            session = rbacService.currentInfo(request, null);
            info = userService.info(DPUtil.parseInt(session.get("uid")));
        }
        if(null != info) {
            info.setPassword("******");
            info.setSalt("******");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("info", info);
        result.put("menu", rbacService.menu(request, DPUtil.parseInt(settingService.get(module, "menu-parent-id"))));
        result.put("resource", rbacService.resource(request));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/logout")
    public String logoutAction(HttpServletRequest request) {
        rbacService.currentInfo(request, DPUtil.buildMap("uid", 0));
        return ApiUtil.echoResult(0, null, null);
    }

}
