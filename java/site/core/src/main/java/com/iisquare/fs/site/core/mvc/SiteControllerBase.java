package com.iisquare.fs.site.core.mvc;

import com.iisquare.fs.site.core.Assets;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;

public abstract class SiteControllerBase extends PermitControllerBase {

    @Autowired
    protected Assets assets;

    @Override
    protected String displayTemplate(ModelMap model, HttpServletRequest request, String controller, String action) {
        model.put("assets", assets);
        return super.displayTemplate(model, request, controller, action);
    }

}
