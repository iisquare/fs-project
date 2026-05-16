package com.iisquare.fs.web.site.mvc;

import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import jakarta.servlet.http.HttpServletRequest;

public abstract class CMSControllerBase extends SiteControllerBase {

    @Autowired
    protected DefaultRbacService rbacService;

    @Override
    protected String displayTemplate(ModelMap model, HttpServletRequest request, String controller, String action) {
        model.put("cms", rbacService.setting("cms", null, null));
        return super.displayTemplate(model, request, controller, action);
    }
}
