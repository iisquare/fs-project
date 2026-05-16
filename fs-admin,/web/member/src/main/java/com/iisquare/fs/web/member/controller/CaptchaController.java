package com.iisquare.fs.web.member.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.member.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    CaptchaService captchaService;

    @GetMapping("/generate")
    public String generateAction(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = captchaService.generate(request, response);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/verify")
    public String verifyAction(@RequestBody Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = captchaService.verify(param);
        return ApiUtil.echoResult(result);
    }

}
