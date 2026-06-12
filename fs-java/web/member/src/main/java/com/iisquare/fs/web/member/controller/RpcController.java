package com.iisquare.fs.web.member.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.RpcControllerBase;
import com.iisquare.fs.web.member.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

}
