package com.iisquare.fs.web.site.web.member;

import com.iisquare.fs.web.site.mvc.MemberControllerBase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/member-web")
public class IndexController extends MemberControllerBase {

    @GetMapping("/")
    public String indexAction(ModelMap model, HttpServletRequest request) throws Exception {
        return displayTemplate(model, request);
    }

}
