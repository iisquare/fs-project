package com.iisquare.fs.web.member.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.RpcControllerBase;
import com.iisquare.fs.web.member.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/rpc")
public class RpcController extends RpcControllerBase {

    @Autowired
    MessageService messageService;

    @PostMapping("/email")
    public String emailAction(@RequestParam Map<String, Object> param, MultipartHttpServletRequest request) {
        Map<String, Object> result = messageService.email(param, request);
        return ApiUtil.echoResult(result);
    }

    @PostMapping("/dingtalk")
    public String dingtalkAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String recipient = DPUtil.parseString(param.get("recipient"));
        String subject = DPUtil.parseString(param.get("subject"));
        String content = DPUtil.parseString(param.get("content"));
        Map<String, Object> result = messageService.dingtalk(recipient, subject, content);
        return ApiUtil.echoResult(result);
    }

    @PostMapping("/wecom")
    public String wecomAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String recipient = DPUtil.parseString(param.get("recipient"));
        String subject = DPUtil.parseString(param.get("subject"));
        String content = DPUtil.parseString(param.get("content"));
        Map<String, Object> result = messageService.wecom(recipient, subject, content);
        return ApiUtil.echoResult(result);
    }

}
