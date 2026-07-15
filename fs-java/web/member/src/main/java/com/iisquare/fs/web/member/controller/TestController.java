package com.iisquare.fs.web.member.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController extends ControllerBase {

    @Autowired
    MemberRpc memberRpc;

    @RequestMapping("/f1")
    public String f1Action(@RequestParam Map<String, Object> param, MultipartHttpServletRequest request) throws IOException {
        MultipartFile[] files = request.getFileMap().values().toArray(new MultipartFile[0]);
        if (files.length > 0) {
            param.put("c", Arrays.toString(files[0].getBytes()));
        }
        return RpcUtil.string(memberRpc.form("/test/f2", param, files));
    }

    @RequestMapping("/f2")
    public String f2Action(@RequestParam Map<String, Object> param, MultipartHttpServletRequest request) throws IOException {
        System.out.println("Map:" + param);
        for (Map.Entry<String, MultipartFile> entry : request.getFileMap().entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        return ApiUtil.echoResult(0, null, request.getParameterNames());
    }

}
