package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.service.DataLogService;
import com.iisquare.fs.web.member.service.RbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataLog")
public class DataLogController extends PermitControllerBase {

    @Autowired
    DataLogService dataLogService;
    @Autowired
    RbacService rbacService;

    @RequestMapping("/check")
    @Permission
    public String checkAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String permits = DPUtil.parseString(param.get("permits"));
        JsonNode result = rbacService.data(request, param, DPUtil.explode(permits));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = dataLogService.search(param, DPUtil.buildMap());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Long> ids = DPUtil.parseLongList(param.get("ids"));
        boolean result = dataLogService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

}
