package com.iisquare.fs.web.site.web.shop;

import com.iisquare.fs.web.site.mvc.ShopControllerBase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/shop-web")
public class IndexController extends ShopControllerBase {

    @GetMapping("/")
    public String indexAction(ModelMap model, HttpServletRequest request) throws Exception {
        return displayTemplate(model, request);
    }

}
