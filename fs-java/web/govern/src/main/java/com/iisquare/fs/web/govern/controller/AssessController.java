package com.iisquare.fs.web.govern.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.govern.service.AssessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/assess")
public class AssessController extends PermitControllerBase {

    @Autowired
    private AssessService assessService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = assessService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = assessService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = assessService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = assessService.delete(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", assessService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/check")
    public String checkAction(@RequestParam Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = assessService.check(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/log")
    @Permission
    public String logAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = assessService.log(param, DPUtil.buildMap());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/clear")
    @Permission
    public String clearAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = assessService.clear(param, request);
        return ApiUtil.echoResult(result);
    }

}
