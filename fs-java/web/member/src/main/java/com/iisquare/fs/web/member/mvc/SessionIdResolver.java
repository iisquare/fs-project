package com.iisquare.fs.web.member.mvc;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.util.List;

/**
 * 重写CookieHttpSessionIdResolver，支持通过Header传递SessionID
 * HeaderHttpSessionIdResolver.xAuthToken()默认Key为X-Auth-Token，
 * 常量统一声明在web.core.FeignInterceptor.HEADER_AUTH_TOKEN
 * 服务之间传递，需通过web.core.FeignInterceptor放行
 */
public class SessionIdResolver implements HttpSessionIdResolver {
    private static final String WRITTEN_SESSION_ID_ATTR = CookieHttpSessionIdResolver.class.getName().concat(".WRITTEN_SESSION_ID_ATTR");
    private final CookieSerializer cookieSerializer = new DefaultCookieSerializer();

    private Duration maxAge;

    public SessionIdResolver(Duration maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        List<String> ids = this.cookieSerializer.readCookieValues(request);
        if (!ids.isEmpty()) return ids;
        // 显式会话头优先于 Authorization：模型网关等场景中 Authorization 可能是 API Key
        String token = request.getHeader(FeignInterceptor.HEADER_AUTH_TOKEN);
        if (!DPUtil.empty(token)) {
            ids.add(token);
            return ids;
        }
        String authorization = request.getHeader("authorization");
        if (null != authorization && authorization.startsWith("Bearer ")) {
            authorization = authorization.substring("Bearer ".length());
            if (!authorization.isEmpty()) {
                ids.add(authorization);
            }
        }
        return ids;
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        if (!sessionId.equals(request.getAttribute(WRITTEN_SESSION_ID_ATTR))) {
            request.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
            writeCookie(request, response, sessionId);
        }
    }

    /**
     * 请求是否携带会话Cookie，用于判断客户端是否为浏览器Cookie方式
     */
    public boolean hasSessionCookie(HttpServletRequest request) {
        return !this.cookieSerializer.readCookieValues(request).isEmpty();
    }

    /**
     * 会话仍然有效时重新下发Cookie，刷新浏览器端Max-Age，避免活跃用户在固定时间点被登出
     */
    public void renewSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        request.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
        writeCookie(request, response, sessionId);
    }

    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        this.cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, ""));
    }

    private void writeCookie(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        CookieSerializer.CookieValue cookie = new CookieSerializer.CookieValue(request, response, sessionId);
        cookie.setCookieMaxAge((int) maxAge.toSeconds());
        this.cookieSerializer.writeCookieValue(cookie);
    }

}
