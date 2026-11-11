package com.iisquare.fs.web.agent.mvc;

import com.iisquare.fs.web.agent.service.ClientVersionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Map;

public class ClientVersionInterceptorTest {

    private ClientVersionInterceptor interceptor() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ClientVersionService> provider = (ObjectProvider<ClientVersionService>) Proxy
                .newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[]{ObjectProvider.class},
                        (proxy, method, args) -> defaultValue(method.getReturnType()));
        return new ClientVersionInterceptor(provider);
    }

    @Test
    public void clientOnlyPathsRejectMissingVersion() throws Exception {
        for (String uri : new String[]{
                "/bot/upload",
                "/skill/available",
                "/skillVersion/available",
                "/skillVersion/package"
        }) {
            FakeResponse response = new FakeResponse();
            Assert.assertFalse(interceptor().preHandle(request("POST", uri, "", Map.of()),
                    response.proxy(), null));
            Assert.assertEquals(uri, 426, response.status);
            Assert.assertTrue(response.body.toString().contains("缺少客户端版本请求头"));
        }
    }

    @Test
    public void adminAndUpdatePathsKeepWorkingWithoutVersion() throws Exception {
        for (String uri : new String[]{
                "/skill/list",
                "/skillVersion/list",
                "/skillVersion/download",
                "/clientUpdate/check"
        }) {
            FakeResponse response = new FakeResponse();
            Assert.assertTrue(interceptor().preHandle(request("POST", uri, "", Map.of()),
                    response.proxy(), null));
            Assert.assertEquals(uri, 200, response.status);
        }
    }

    @Test
    public void contextPathDoesNotHideClientOnlyPath() throws Exception {
        FakeResponse response = new FakeResponse();
        Assert.assertFalse(interceptor().preHandle(
                request("GET", "/agent/skillVersion/package", "/agent", Map.of()),
                response.proxy(), null));
        Assert.assertEquals(426, response.status);
    }

    private HttpServletRequest request(String method, String uri, String contextPath,
            Map<String, String> headers) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, called, args) -> {
                    switch (called.getName()) {
                        case "getMethod":
                            return method;
                        case "getRequestURI":
                            return uri;
                        case "getContextPath":
                            return contextPath;
                        case "getHeader":
                            return headers.get((String) args[0]);
                        default:
                            return defaultValue(called.getReturnType());
                    }
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (boolean.class == type) return false;
        if (char.class == type) return '\0';
        if (byte.class == type) return (byte) 0;
        if (short.class == type) return (short) 0;
        if (int.class == type) return 0;
        if (long.class == type) return 0L;
        if (float.class == type) return 0F;
        if (double.class == type) return 0D;
        return null;
    }

    private static class FakeResponse {
        int status = 200;
        final StringWriter body = new StringWriter();
        final PrintWriter writer = new PrintWriter(body);

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setStatus":
                                status = (Integer) args[0];
                                return null;
                            case "getStatus":
                                return status;
                            case "getWriter":
                                return writer;
                            default:
                                return defaultValue(method.getReturnType());
                        }
                    });
        }
    }
}
