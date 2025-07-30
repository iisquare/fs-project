package com.iisquare.fs.web.site.wap.bbs;

import com.iisquare.fs.web.site.mvc.BBSControllerBase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/bbs-wap")
public class IndexController extends BBSControllerBase {

    @GetMapping("/")
    public String indexAction(ModelMap model, HttpServletRequest request) throws Exception {
        return displayTemplate(model, request);
    }

}
