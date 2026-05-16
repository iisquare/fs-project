package com.iisquare.fs.web.lm.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.service.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController extends PermitControllerBase {

    @Autowired
    private GatewayService gatewayService;

    @RequestMapping("/notice")
    @Permission("notice")
    public String noticeAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = gatewayService.notice(param);
        return ApiUtil.echoResult(result);
    }

}
