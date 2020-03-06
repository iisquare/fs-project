package com.iisquare.fs.web.core.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.web.mvc.ServiceBase;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public abstract class RbacServiceBase extends ServiceBase {

    public int uid(HttpServletRequest request) {
        return currentInfo(request).at("/uid").asInt();
    }

    public String keyPermit(String module, String controller, String action) {
        return module + ":" + controller + ":" + action;
    }

    public boolean hasPermit(HttpServletRequest request, String controller, String action) {
        String module = request.getAttribute("module").toString();
        return hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request, String action) {
        String module = request.getAttribute("module").toString();
        String controller = request.getAttribute("controller").toString();
        return hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request) {
        String module = request.getAttribute("module").toString();
        String controller = request.getAttribute("controller").toString();
        String action = request.getAttribute("action").toString();
        return hasPermit(request, module, controller, action);
    }

    public abstract JsonNode currentInfo(HttpServletRequest request);

    public abstract boolean hasPermit(HttpServletRequest request, String module, String controller, String action);

    public abstract boolean hasPermit(HttpServletRequest request, Map<String, Boolean> name2boolean);

    public abstract JsonNode resource(HttpServletRequest request, Map<String, Boolean> name2boolean);

    public abstract JsonNode menu(HttpServletRequest request, Integer parentId);

}
