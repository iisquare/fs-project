package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/log")
public class LogController extends PermitControllerBase {

    @Autowired
    private LogService logService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = logService.search(param, DPUtil.buildMap("withInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/audit")
    @Permission
    public String auditAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = logService.audit(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Long> ids = DPUtil.parseLongList(param.get("ids"));
        boolean result = logService.delete(ids, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

}
