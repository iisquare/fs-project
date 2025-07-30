package com.iisquare.fs.web.site.mvc;

import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.site.core.Assets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import jakarta.servlet.http.HttpServletRequest;

public abstract class SiteControllerBase extends ControllerBase {

    @Autowired
    protected Assets assets;

    protected String displayTemplate(ModelMap model, HttpServletRequest request) {
        String controller = request.getAttribute(SiteInterceptor.ATTRIBUTE_CONTROLLER).toString();
        String action = request.getAttribute(SiteInterceptor.ATTRIBUTE_ACTION).toString();
        return displayTemplate(model, request, controller, action);
    }

    protected String displayTemplate(ModelMap model, HttpServletRequest request, String action) {
        String controller = request.getAttribute(SiteInterceptor.ATTRIBUTE_CONTROLLER).toString();
        return displayTemplate(model, request, controller, action);
    }

    protected String displayTemplate(ModelMap model, HttpServletRequest request, String controller, String action) {
        model.put("assets", assets);
        String template = request.getAttribute(SiteInterceptor.ATTRIBUTE_TEMPLATE).toString();
        return template + "/" + controller + "/" + action;
    }

    protected String redirect(String url) throws Exception {
        return "redirect:" + url;
    }

    protected String forward(String url) throws Exception {
        return "forward:" + url;
    }

}
