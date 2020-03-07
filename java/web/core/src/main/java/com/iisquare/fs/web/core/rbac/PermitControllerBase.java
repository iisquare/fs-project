package com.iisquare.fs.web.core.rbac;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;

public abstract class PermitControllerBase extends ControllerBase {

    protected String displayTemplate(ModelMap model, HttpServletRequest request) {
        String controller = request.getAttribute(PermitInterceptor.ATTRIBUTE_CONTROLLER).toString();
        String action = request.getAttribute(PermitInterceptor.ATTRIBUTE_ACTION).toString();
        return displayTemplate(model, request, controller, action);
    }

    protected String displayTemplate(ModelMap model, HttpServletRequest request, String action) {
        String controller = request.getAttribute(PermitInterceptor.ATTRIBUTE_CONTROLLER).toString();
        return displayTemplate(model, request, controller, action);
    }

    protected String displayTemplate(ModelMap model, HttpServletRequest request, String controller, String action) {
        String module = request.getAttribute(PermitInterceptor.ATTRIBUTE_MODULE).toString();
        if(DPUtil.empty(module)) return controller + "/" + action;
        return module + "/" + controller + "/" + action;
    }


    protected String redirect(String url) throws Exception {
        return "redirect:" + url;
    }

    protected String forward(String url) throws Exception {
        return "forward:" + url;
    }

}
