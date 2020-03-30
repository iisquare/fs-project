package com.iisquare.fs.web.admin.mvc;

import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.admin.service.SessionService;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.admin.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public abstract class PermitController extends ControllerBase {

    @Autowired
    protected SessionService sessionService;
    @Autowired
    private SettingService settingService;

    public int uid(HttpServletRequest request) {
        return DPUtil.parseInt(sessionService.currentInfo(request, null).get("uid"));
    }

    public boolean hasPermit(HttpServletRequest request, String module, String controller, String action) {
        return sessionService.hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request, String controller, String action) {
        String module = request.getAttribute("module").toString();
        return sessionService.hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request, String action) {
        String module = request.getAttribute("module").toString();
        String controller = request.getAttribute("controller").toString();
        return sessionService.hasPermit(request, module, controller, action);
    }

    public boolean hasPermit(HttpServletRequest request) {
        String module = request.getAttribute("module").toString();
        String controller = request.getAttribute("controller").toString();
        String action = request.getAttribute("action").toString();
        return sessionService.hasPermit(request, module, controller, action);
    }

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
        Map<String, Object> page = new HashMap<>();
        page.put("title", settingService.get("system", "siteName"));
        model.put("page", page);

        String manageUrl = settingService.get("system", "manageUrl");
        if(DPUtil.empty(manageUrl)) manageUrl = "/manage";
        model.put("manageUrl", manageUrl);
        String staticUrl = settingService.get("system", "staticUrl");
        if(DPUtil.empty(staticUrl)) staticUrl = "/static";
        model.put("staticUrl", staticUrl);

        if(DPUtil.empty(controller)) {
            return module + "/" + action;
        }
        return module + "/" + controller + "/" + action;
    }

}
