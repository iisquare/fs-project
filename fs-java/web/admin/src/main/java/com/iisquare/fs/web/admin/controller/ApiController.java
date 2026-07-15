package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.admin.service.ApiService;
import org.apache.http.client.methods.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * 统一API代理控制器，HTTP请求直接转发到后端服务
 *
 * 所有请求体和响应体均以流式透传，不解析、不编码：
 * - /api/{appName}/**  任意HTTP方法，请求体和响应体原样转发
 * - JSON、form、multipart、SSE、文件下载 全部统一处理
 *
 * 后端服务通过 rpc.{appName}.rest 属性配置：
 *   rpc.member.rest = http://127.0.0.1:7801
 *
 * 要求：spring.servlet.multipart.enabled = false （禁用MultipartResolver，保持原始流可用）
 */
@RestController
@RequestMapping("/api")
public class ApiController extends ControllerBase {

    @Autowired
    ApiService apiService;

    @RequestMapping(value = "/{appName}/**")
    public void proxy(@PathVariable String appName, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String appPath = apiService.extractPath(request);
        String backendUrl = apiService.resolveBackendUrl(appName);
        if (DPUtil.empty(backendUrl)) {
            displayJSON(response, ApiUtil.result(15404, "后端服务未配置: " + appName, null));
            return;
        }
        String fullUrl = apiService.buildUrl(backendUrl, appPath, request.getQueryString());
        HttpRequestBase httpRequest = apiService.buildBackendRequest(fullUrl, request);
        if (null == httpRequest) {
            displayJSON(response, ApiUtil.result(15401, "不支持的HTTP方法: " + request.getMethod(), null));
            return;
        }
        apiService.applyHeaders(httpRequest, request);
        apiService.applyTimeout(httpRequest, appName);
        apiService.executeAndStream(httpRequest, response);
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setCharacterEncoding(charset);
        response.setContentType("application/json; charset=" + charset);
        String message = e.getMessage();
        if (DPUtil.empty(message) && e.getCause() != null) {
            message = e.getCause().getMessage();
        }
        response.getWriter().write(ApiUtil.echoResult(15503, "网关服务异常", message));
    }

}
