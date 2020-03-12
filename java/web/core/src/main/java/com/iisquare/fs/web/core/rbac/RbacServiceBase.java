package com.iisquare.fs.web.core.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public abstract class RbacServiceBase extends ServiceBase {

    public int uid(HttpServletRequest request) {
        return currentInfo(request).at("/uid").asInt();
    }

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

    public abstract JsonNode menu(HttpServletRequest request, Integer parentId);

}
