package com.iisquare.fs.site.frontend.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.site.core.service.CaptchaService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/captcha")
public class CaptchController extends PermitControllerBase {

    @Autowired
    CaptchaService captchaService;

    @RequestMapping("/generate")
    public String generateAction(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        Map<String, Object> result = captchaService.generate(params, request);
        return ApiUtil.echoResult(result);
    }

}
