package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.web.admin.mvc.PermitController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RequestMapping("/manage")
@Controller
public class IndexController extends PermitController {

    @RequestMapping("/")
    public String indexAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        return displayTemplate(model, request, "index", "index");
    }

}
