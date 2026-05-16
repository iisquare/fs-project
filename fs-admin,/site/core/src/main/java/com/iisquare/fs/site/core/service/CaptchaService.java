package com.iisquare.fs.site.core.service;

import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CaptchaService {

    @Autowired
    MemberRpc memberRpc;

    public Map<String, Object> get(String uri, Map<String, Object> params) {
        return RpcUtil.result(memberRpc.get(uri, params));
    }

    public Map<String, Object> generate(Map<String, Object> params, HttpServletRequest request) {
        return get("/captcha/generate", params);
    }

}
