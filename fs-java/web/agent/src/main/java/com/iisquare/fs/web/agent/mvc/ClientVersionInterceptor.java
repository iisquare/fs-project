package com.iisquare.fs.web.agent.mvc;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.ClientVersionService;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 客户端业务接口的版本闸门。
 *
 * 只处理携带 x-client-version 的客户端请求；管理后台与未携带版本头的旧调用保持原行为。
 * 低于最低支持版本或命中回滚目标时返回 426，客户端据此强制升级。
 */
public class ClientVersionInterceptor implements HandlerInterceptor {

    public static final String HEADER_VERSION = FeignInterceptor.HEADER_CLIENT_VERSION;
    public static final String HEADER_PLATFORM = FeignInterceptor.HEADER_CLIENT_PLATFORM;
    public static final String HEADER_ARCH = FeignInterceptor.HEADER_CLIENT_ARCH;
    public static final String HEADER_CLIENT_ID = FeignInterceptor.HEADER_CLIENT_ID;
    public static final String HEADER_CHANNEL = FeignInterceptor.HEADER_CLIENT_CHANNEL;

    private final ObjectProvider<ClientVersionService> clientVersionServiceProvider;

    public ClientVersionInterceptor(ObjectProvider<ClientVersionService> clientVersionServiceProvider) {
        this.clientVersionServiceProvider = clientVersionServiceProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String uri = request.getRequestURI();
        if (uri.startsWith("/clientUpdate")) return true;
        String version = DPUtil.trim(request.getHeader(HEADER_VERSION));
        if (DPUtil.empty(version)) {
            // 客户端专用 API 必须携带版本；管理后台与其它内部调用不受影响
            if (uri.startsWith("/bot")) return reject(response, "缺少客户端版本请求头");
            return true;
        }
        boolean blocked = clientVersionServiceProvider.getObject().accessBlocked(DPUtil.buildMap(
                "currentVersion", version,
                "platform", request.getHeader(HEADER_PLATFORM),
                "architecture", request.getHeader(HEADER_ARCH),
                "clientId", request.getHeader(HEADER_CLIENT_ID),
                "channel", request.getHeader(HEADER_CHANNEL)
        ));
        if (!blocked) return true;
        return reject(response, "客户端版本过低，必须升级");
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(426);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(ApiUtil.echoResult(ApiUtil.result(90426, message, null)));
        return false;
    }

}
