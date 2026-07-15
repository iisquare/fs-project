package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.entity.Usage;
import com.iisquare.fs.web.lm.service.UsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usage")
public class UsageController extends PermitControllerBase {

    @Autowired
    UsageService usageService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        long id = DPUtil.parseLong(param.get("id"));
        Usage info = usageService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = usageService.search(param, DPUtil.buildMap("withInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/audit")
    @Permission
    public String auditAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = usageService.audit(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Long> ids = DPUtil.parseLongList(param.get("ids"));
        boolean result = usageService.delete(ids, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/statistic")
    @Permission("")
    public String statisticAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = usageService.statistic(param);
        return ApiUtil.echoResult(result);
    }

}
