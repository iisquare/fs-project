package com.iisquare.fs.web.admin.mvc;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.core.rbac.PermitInterceptor;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;

public class AdminControllerBase extends PermitControllerBase {

    @Override
    protected String displayTemplate(ModelMap model, HttpServletRequest request, String controller, String action) {
        model.put("adminUrl", "");
        model.put("staticUrl", "/static");
        String template = request.getAttribute(PermitInterceptor.ATTRIBUTE_TEMPLATE).toString();
        if(DPUtil.empty(template)) return controller + "/" + action;
        return template + "/" + controller + "/" + action;
    }

}
