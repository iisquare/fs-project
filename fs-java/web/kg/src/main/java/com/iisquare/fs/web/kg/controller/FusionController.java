package com.iisquare.fs.web.kg.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.kg.service.FusionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 知识融合：同本体内实体去重，候选一律人工确认
 */
@RestController
@RequestMapping("/fusion")
public class FusionController extends PermitControllerBase {

    @Autowired
    FusionService fusionService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/ruleList")
    @Permission("kg:fusion:")
    public String ruleListAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(fusionService.ruleList(param));
    }

    @RequestMapping("/ruleSave")
    @Permission("kg:fusion:scan")
    public String ruleSaveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return ApiUtil.echoResult(fusionService.ruleSave(param, rbacService.uid(request)));
    }

    @RequestMapping("/ruleDelete")
    @Permission("kg:fusion:scan")
    public String ruleDeleteAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(fusionService.ruleDelete(param));
    }

    @RequestMapping("/scan")
    @Permission("kg:fusion:scan")
    public String scanAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return ApiUtil.echoResult(fusionService.scan(param, rbacService.uid(request)));
    }

    @RequestMapping("/candidateList")
    @Permission("kg:fusion:")
    public String candidateListAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = fusionService.candidateSearch(param);
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/candidateDetail")
    @Permission("kg:fusion:")
    public String candidateDetailAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(fusionService.candidateDetail(param));
    }

    @RequestMapping("/candidateReject")
    @Permission("kg:fusion:merge")
    public String candidateRejectAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return ApiUtil.echoResult(fusionService.reject(param, rbacService.uid(request)));
    }

    @RequestMapping("/merge")
    @Permission("kg:fusion:merge")
    public String mergeAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return ApiUtil.echoResult(fusionService.merge(param, rbacService.uid(request)));
    }

    @RequestMapping("/recordList")
    @Permission("kg:fusion:")
    public String recordListAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = fusionService.recordSearch(param);
        return ApiUtil.echoResult(0, null, result);
    }

}
