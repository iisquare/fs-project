package com.iisquare.fs.web.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具会话的共用方法：从鉴权接口取调用方登录会话、校验登录用户、按工具开关透传会话。
 *
 * - MCP 工具方法（webSearch / webFetch / multimodalAnalyze）调用 {@link #requireSession()}：
 *   没有会话或会话无效时直接在工具方法里拒绝，不依赖端点拦截器；
 * - 调用任意工具服务（MCP / OpenAPI）时由 {@link #headers(Map, boolean, HttpServletRequest)} 统一处理：
 *   开关为 true 才补 {@code x-auth-token}，为 false 绝不主动添加；
 *   开关 false 时仍会剔除误填进认证 header 的当前调用方会话值，避免泄露。
 */
@Component
public class ToolSessionSupport {

    private static final Logger logger = LoggerFactory.getLogger(ToolSessionSupport.class);

    @Autowired
    DefaultRbacService rbacService;

    /**
     * 当前 MCP 工具调用的登录会话，由鉴权接口统一解析 Cookie / Authorization / x-auth-token。
     * 没有登录会话、会话过期或用户不可用时抛错，由 MCP 工具把原因返回给调用方。
     */
    public String requireSession() {
        HttpServletRequest request = currentRequest();
        if (null == request) {
            throw new IllegalStateException("MCP 工具需要登录会话（未获取到当前请求）");
        }
        String token;
        try {
            token = rbacService.token(request);
        } catch (Exception e) {
            logger.warn("MCP session resolve failed", e);
            throw new IllegalStateException("MCP 工具登录会话校验暂不可用，请稍后重试");
        }
        if (DPUtil.empty(token)) {
            throw new IllegalStateException(
                    "MCP 工具需要登录会话（Cookie、Authorization: Bearer 或 x-auth-token）");
        }
        JsonNode identity;
        try {
            identity = rbacService.identityByToken(token);
        } catch (Exception e) {
            // 校验失败的内部原因只留在服务端日志，不把 Feign / 地址等细节回给模型
            logger.warn("MCP session validation failed", e);
            throw new IllegalStateException("MCP 工具登录会话校验暂不可用，请稍后重试");
        }
        if (null == identity || identity.isEmpty()) {
            throw new IllegalStateException("MCP 工具登录会话无效或已过期");
        }
        return token;
    }

    /** 实际发送给 MCP 服务的请求头：是否补会话完全由工具配置开关决定 */
    public Map<String, String> headers(Map<String, String> configured, boolean passSession) {
        return headers(configured, passSession, currentRequest());
    }

    public Map<String, String> headers(Map<String, String> configured, boolean passSession,
                                       HttpServletRequest request) {
        if (null == request) request = currentRequest();
        Map<String, String> headers = null == configured
                ? new LinkedHashMap<>() : new LinkedHashMap<>(configured);
        // passSession 关闭且没有需要清理的认证头时，不必为取 token 额外调用鉴权服务
        String token = passSession || headers.keySet().stream().anyMatch(this::sessionHeader)
                ? rbacService.token(request) : "";
        if (passSession) {
            // 以调用方会话为准：先移除配置里大小写不同的同名头，避免重复或旧值抢先
            headers.keySet().removeIf(name -> FeignInterceptor.HEADER_AUTH_TOKEN.equalsIgnoreCase(name));
            if (!DPUtil.empty(token)) headers.put(FeignInterceptor.HEADER_AUTH_TOKEN, token);
            return headers;
        }
        // 开关关闭时绝不主动传会话；同时剔除误填进认证 header 的当前调用方会话值
        if (!DPUtil.empty(token)) {
            headers.entrySet().removeIf(entry -> sessionHeader(entry.getKey())
                    && null != entry.getValue() && entry.getValue().contains(token));
        }
        return headers;
    }

    protected boolean sessionHeader(String name) {
        if (null == name) return false;
        return "authorization".equalsIgnoreCase(name)
                || FeignInterceptor.HEADER_AUTH_TOKEN.equalsIgnoreCase(name)
                || "cookie".equalsIgnoreCase(name);
    }

    protected HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes
                ? ((ServletRequestAttributes) attributes).getRequest() : null;
    }

}
