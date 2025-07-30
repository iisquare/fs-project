package com.iisquare.fs.web.core.mvc;

import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.PermitRpc;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

public class FeignInterceptor implements RequestInterceptor {

    @Value("${spring.application.name}")
    private String appName;
    private static String secret;
    public static final String HEADER_APP_NAME = "fs-app-name";
    public static final String HEADER_APP_TIME = "fs-app-time";
    public static final String HEADER_APP_TOKEN = "fs-app-token";
    public static final List<String> headers = Arrays.asList("x-auth-token", "cookie", "user-agent", "authorization");

    @Value("${fs.rpc.secret}")
    public void setSecret(String secret) {
        FeignInterceptor.secret = secret;
    }

    public static boolean hashPermit(HttpServletRequest request, HttpServletResponse response, HandlerMethod method) {
        if (null == method.getMethodAnnotation(PermitRpc.class)
                && null == method.getBeanType().getAnnotation(PermitRpc.class)) {
            return true;
        }
        String name = request.getHeader(FeignInterceptor.HEADER_APP_NAME);
        if ("fs-admin-service".equals(name)) return false; // 禁用admin后台的转发
        String time = request.getHeader(FeignInterceptor.HEADER_APP_TIME);
        String token = request.getHeader(FeignInterceptor.HEADER_APP_TOKEN);
        if (DPUtil.empty(name) || DPUtil.empty(time)) return false;
        if (Math.abs(System.currentTimeMillis() - DPUtil.parseLong(time)) > 3000) return false;
        return token(name, time).equals(token);
    }

    public static String token(String name, String time) {
        return CodeUtil.md5(CodeUtil.md5(CodeUtil.md5(name) + time) + secret).substring(0, 6);
    }

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (null == attributes) return;
        HttpServletRequest request = attributes.getRequest();
        for (String name : headers) {
            String header = request.getHeader(name);
            if (!DPUtil.empty(header)) {
                template.header(name, header);
            }
        }
        String time = String.valueOf(System.currentTimeMillis());
        template.header(HEADER_APP_NAME, appName);
        template.header(HEADER_APP_TIME, time);
        template.header(HEADER_APP_TOKEN, token(appName, time));
    }

}
