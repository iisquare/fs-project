package com.iisquare.fs.web.agent.core;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本次 agent 运行访问 LM 时使用的认证信息。
 *
 * agent 对 LM 始终使用配置中的服务密钥；外部调用只负责在 agent 内完成调用方鉴权，
 * 再把可选用户会话和追踪标识传给 LM。
 * 上下文按线程保存，并行迭代必须显式继承。
 */
@Component
public class AgentGatewayAuth {

    public record Context(String session, String traceId) {
    }

    private final ThreadLocal<Context> current = new ThreadLocal<>();

    public Context get() {
        return current.get();
    }

    public void set(Context context) {
        if (null == context) {
            current.remove();
        } else {
            current.set(context);
        }
    }

    public void clear() {
        current.remove();
    }

    /**
     * 构建访问 LM 的认证头。存在入站 API Key 时不能再带服务级 Authorization，
     * 否则 LM 会优先按 Authorization 解析成服务密钥。
     */
    public Map<String, String> headers(String serviceToken, String session) {
        Map<String, String> headers = new LinkedHashMap<>();
        Context context = get();
        if (!DPUtil.empty(serviceToken)) {
            headers.put("Authorization", "Bearer " + serviceToken);
        }
        String traceId = null == context ? "" : DPUtil.parseString(context.traceId());
        if (!DPUtil.empty(traceId)) headers.put("x-trace-id", traceId);
        String activeSession = DPUtil.empty(session) && null != context
                ? DPUtil.parseString(context.session()) : DPUtil.parseString(session);
        if (!DPUtil.empty(activeSession)) {
            headers.put(FeignInterceptor.HEADER_AUTH_TOKEN, activeSession);
        }
        return headers;
    }

}
