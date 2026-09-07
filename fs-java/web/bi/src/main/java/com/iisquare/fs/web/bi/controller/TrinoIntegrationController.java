package com.iisquare.fs.web.bi.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.bi.service.TrinoIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/integration/trino")
public class TrinoIntegrationController extends ControllerBase {

    @Autowired
    TrinoIntegrationService trinoIntegrationService;

    @PostMapping("/tables")
    public String tablesAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = trinoIntegrationService.tables(param, request);
        return ApiUtil.echoResult(result);
    }

    @PostMapping("/data")
    public String dataAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = trinoIntegrationService.data(param, request);
        return ApiUtil.echoResult(result);
    }
}
