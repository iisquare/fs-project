package com.iisquare.fs.web.agent.core;

import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class AgentGatewayAuthTest {

    private AgentGatewayAuth auth;

    @Before
    public void setUp() {
        auth = new AgentGatewayAuth();
    }

    @Test
    public void serviceTokenIsUsedWithoutContext() {
        Map<String, String> headers = auth.headers("service-token", "member-session");

        Assert.assertEquals("Bearer service-token", headers.get("Authorization"));
        Assert.assertEquals("member-session", headers.get(FeignInterceptor.HEADER_AUTH_TOKEN));
    }

    @Test
    public void contextAddsUserSessionAndTrace() {
        auth.set(new AgentGatewayAuth.Context("member-session", "trace-1"));

        Map<String, String> headers = auth.headers("service-token", null);

        Assert.assertEquals("Bearer service-token", headers.get("Authorization"));
        Assert.assertEquals("member-session", headers.get(FeignInterceptor.HEADER_AUTH_TOKEN));
        Assert.assertEquals("trace-1", headers.get("x-trace-id"));
        Assert.assertNull(headers.get("x-api-key"));
        Assert.assertNull(headers.get("x-proxy-key"));
    }

    @Test
    public void explicitSessionTakesPrecedenceOverContextSession() {
        auth.set(new AgentGatewayAuth.Context("context-session", "trace-3"));

        Map<String, String> headers = auth.headers("service-token", "explicit-session");

        Assert.assertEquals("explicit-session", headers.get(FeignInterceptor.HEADER_AUTH_TOKEN));
    }

}
