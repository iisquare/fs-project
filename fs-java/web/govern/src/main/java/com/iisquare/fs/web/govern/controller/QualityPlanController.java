package com.iisquare.fs.web.govern.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.govern.service.QualityPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qualityPlan")
public class QualityPlanController extends PermitControllerBase {

    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private QualityPlanService planService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = planService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = planService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = planService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", planService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/check")
    public String checkAction(@RequestParam Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = planService.check(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/log")
    @Permission
    public String logAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = planService.log(param, DPUtil.buildMap(
                "withLogicInfo", true, "withRuleInfo", true, "withPlanInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/clear")
    @Permission
    public String clearAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = planService.clear(param, request);
        return ApiUtil.echoResult(result);
    }

}
