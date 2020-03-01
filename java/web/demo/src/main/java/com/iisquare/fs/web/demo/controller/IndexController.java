package com.iisquare.fs.web.demo.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RequestMapping("/")
@RestController
public class IndexController extends ControllerBase {

    @RequestMapping("/")
    public String indexAction(ModelMap model, HttpServletRequest request) {
        return ApiUtil.echoResult(0, "it's work!", null);
    }

}
