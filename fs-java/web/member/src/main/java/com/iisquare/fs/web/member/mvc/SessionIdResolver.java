package com.iisquare.fs.web.member.mvc;

import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 重写CookieHttpSessionIdResolver，支持通过Header传递SessionID
 * HeaderHttpSessionIdResolver.xAuthToken()默认Key为X-Auth-Token
 * 服务之间传递，需通过web.core.FeignInterceptor放行
 */
public class SessionIdResolver implements HttpSessionIdResolver {
    private static final String WRITTEN_SESSION_ID_ATTR = CookieHttpSessionIdResolver.class.getName().concat(".WRITTEN_SESSION_ID_ATTR");
    private final CookieSerializer cookieSerializer = new DefaultCookieSerializer();

    public SessionIdResolver() {
    }

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        List<String> ids = this.cookieSerializer.readCookieValues(request);
        if (!ids.isEmpty()) return ids;
        String authorization = request.getHeader("authorization");
        if (null != authorization && authorization.startsWith("Bearer ")) {
            authorization = authorization.substring("Bearer ".length());
            if (!authorization.isEmpty()) {
                ids.add(authorization);
            }
        }
        if (!ids.isEmpty()) return ids;
        String token = request.getHeader("x-auth-token");
        if (!DPUtil.empty(token)) {
            ids.add(token);
        }
        return ids;
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        if (!sessionId.equals(request.getAttribute(WRITTEN_SESSION_ID_ATTR))) {
            request.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
            this.cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, sessionId));
        }
    }

    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        this.cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, ""));
    }

}
