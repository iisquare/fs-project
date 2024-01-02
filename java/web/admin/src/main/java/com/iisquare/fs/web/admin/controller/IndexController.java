package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.admin.mvc.AdminControllerBase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/")
public class IndexController extends AdminControllerBase {

    @GetMapping("/")
    public String indexAction(ModelMap model, HttpServletRequest request) throws Exception {
        return displayTemplate(model, request);
    }

    @RequestMapping("/state")
    public String stateAction(HttpServletResponse response) throws Exception {
        return displayText(response, ApiUtil.echoResult(0, null, null));
    }

}
