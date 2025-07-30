package com.iisquare.fs.web.cron.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.cron.service.TriggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/trigger")
public class TriggerController extends PermitControllerBase {

    @Autowired
    private TriggerService triggerService;

    @RequestMapping("/list")
    @Permission("job:")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result;
        try {
            result = triggerService.search(param, DPUtil.buildMap());
        } catch (Exception e) {
            return ApiUtil.echoResult(5000, "获取查询结果失败", e.getMessage());
        }
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission("job:")
    public String saveAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = triggerService.save(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/command")
    @Permission("job:")
    public String commandAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = triggerService.command(param);
        return ApiUtil.echoResult(result);
    }

}
