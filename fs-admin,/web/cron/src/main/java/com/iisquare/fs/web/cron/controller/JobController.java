package com.iisquare.fs.web.cron.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.cron.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/job")
public class JobController extends PermitControllerBase {

    @Autowired
    private JobService jobService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result;
        try {
            result = jobService.search(param, DPUtil.buildMap());
        } catch (Exception e) {
            return ApiUtil.echoResult(5000, "获取查询结果失败", e.getMessage());
        }
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = jobService.save(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/command")
    @Permission("")
    public String commandAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        String command = DPUtil.trim(DPUtil.parseString(param.get("command")));
        if ("delete".equals(command) && !rbacService.hasPermit(request, "delete")) {
            return ApiUtil.echoResult(9403, null, null);
        }
        Map<String, Object> result = jobService.command(param);
        return ApiUtil.echoResult(result);
    }

}
