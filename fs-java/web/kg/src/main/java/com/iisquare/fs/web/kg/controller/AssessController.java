package com.iisquare.fs.web.kg.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.kg.service.AssessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 知识评估：按本体定义检查图数据质量
 */
@RestController
@RequestMapping("/assess")
public class AssessController extends PermitControllerBase {

    @Autowired
    AssessService assessService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/run")
    @Permission("kg:assess:run")
    public String runAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return ApiUtil.echoResult(assessService.assess(param, rbacService.uid(request)));
    }

    @RequestMapping("/history")
    @Permission("kg:assess:history")
    public String historyAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(assessService.history(param));
    }

    @RequestMapping("/detail")
    @Permission("kg:assess:history")
    public String detailAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(assessService.detail(param));
    }

}
