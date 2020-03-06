package com.iisquare.fs.web.core.rbac;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;

public abstract class PermitControllerBase extends ControllerBase {

    protected String displayTemplate(ModelMap model, HttpServletRequest request) {
        String controller = request.getAttribute("controller").toString();
        String action = request.getAttribute("action").toString();
        return displayTemplate(model, request, controller, action);
    }

    protected String displayTemplate(ModelMap model, HttpServletRequest request, String action) {
        String controller = request.getAttribute("controller").toString();
        return displayTemplate(model, request, controller, action);
    }

    protected String displayTemplate(ModelMap model, HttpServletRequest request, String controller, String action) {
        String module = request.getAttribute("module").toString();
        if(DPUtil.empty(module)) return controller + "/" + action;
        return module + "/" + controller + "/" + action;
    }

}
