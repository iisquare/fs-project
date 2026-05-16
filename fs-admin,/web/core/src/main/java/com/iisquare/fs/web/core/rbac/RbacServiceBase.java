package com.iisquare.fs.web.core.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServletUtil;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class RbacServiceBase extends ServiceBase {

    @Value("${spring.application.name}")
    public String appName;

    public int uid(HttpServletRequest request) {
        return currentInfo(request).at("/uid").asInt();
    }

    public Set<Integer> roleIds(HttpServletRequest request) {
        return DPUtil.values(identity(request).at("/roles"), Integer.class, "id");
    }

    @Deprecated
    public abstract <T> List<T> fillUserInfo(List<T> list, String ...properties);

    public abstract JsonNode fillUserInfo(JsonNode json, String... properties);

    public abstract JsonNode fillUserInfo(String fromSuffix, String toSuffix, JsonNode json, String... properties);

    public String keyPermit(String module, String controller, String action) {
        if (null == controller) controller = "";
        if (null == action) action = "";
        return module + ":" + controller + ":" + action;
    }

    public boolean hasPermit(HttpServletRequest request, String controller, String action) {
        String module = request.getAttribute(PermitInterceptor.ATTRIBUTE_MODULE).toString();
        return hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request, String action) {
        String module = request.getAttribute(PermitInterceptor.ATTRIBUTE_MODULE).toString();
        String controller = request.getAttribute(PermitInterceptor.ATTRIBUTE_CONTROLLER).toString();
        return hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request) {
        String module = request.getAttribute(PermitInterceptor.ATTRIBUTE_MODULE).toString();
        String controller = request.getAttribute(PermitInterceptor.ATTRIBUTE_CONTROLLER).toString();
        String action = request.getAttribute(PermitInterceptor.ATTRIBUTE_ACTION).toString();
        return hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request, String module, String controller, String action) {
        JsonNode resource = resource(request, null);
        String key = keyPermit(module, controller, action);
        return resource.has(key) ? resource.get(key).asBoolean() : false;
    }

    public boolean hasPermit(HttpServletRequest request, Map<String, Boolean> name2boolean) {
        if (null == name2boolean || name2boolean.isEmpty()) return false;
        JsonNode resource = resource(request, null);
        for (Map.Entry<String, Boolean> entry : name2boolean.entrySet()) {
            String key = entry.getKey();
            if (resource.has(key) && resource.get(key).asBoolean()) return true;
        }
        return false;
    }

    public abstract JsonNode currentInfo(HttpServletRequest request);

    public abstract JsonNode resource(HttpServletRequest request);

    public JsonNode resource(HttpServletRequest request, Map<String, Boolean> name2boolean) {
        JsonNode resource = resource(request);
        if (null == name2boolean || name2boolean.isEmpty()) return resource;
        ObjectNode result = DPUtil.objectNode();
        for (Map.Entry<String, Boolean> entry : name2boolean.entrySet()) {
            String key = entry.getKey();
            result.put(key, result.has(key) ? result.get(key).asBoolean() : entry.getValue());
        }
        return result;
    }

    public abstract JsonNode menu(HttpServletRequest request);

    public abstract Map<String, String> setting(String type, List<String> include, List<String> exclude);

    public abstract int setting(String type, Map<String, String> data);

    public abstract JsonNode data(HttpServletRequest request, Object params, String... permits);

    public ObjectNode logParams(HttpServletRequest request) {
        ObjectNode node = DPUtil.objectNode();
        node.put("appName", appName);
        node.put("requestIp", ServletUtil.getRemoteAddr(request));
        node.put("requestUrl", ServletUtil.getFullUrl(request, true, true));
        node.replace("requestHeaders", DPUtil.toJSON(ServletUtil.headers(request)));
        return node;
    }

    public abstract JsonNode identity(HttpServletRequest request);

}
