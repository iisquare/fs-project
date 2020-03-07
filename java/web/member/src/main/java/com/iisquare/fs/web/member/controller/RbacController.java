package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.PermitInterceptor;
import com.iisquare.fs.web.member.service.RbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/rbac")
public class RbacController {

    @Autowired
    private RbacService rbacService;

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

    @PostMapping("/currentInfo")
    public String currentInfoAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.currentInfo(request));
    }

    @PostMapping("/resource")
    public String resourceAction(@RequestBody Map<String, Boolean> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.resource(request, param));
    }

    @PostMapping("/menu")
    public String menuAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        int parentId = ValidateUtil.filterInteger(param.get("parentId"), true, 0, null, 0);
        return ApiUtil.echoResult(0, null, rbacService.menu(request, parentId));
    }

    @PostMapping("/hasPermit")
    public String hasPermitAction(@RequestBody Map<String, Boolean> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, rbacService.hasPermit(request, param));
    }

}
