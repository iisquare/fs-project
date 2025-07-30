package com.iisquare.fs.web.site.rest;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.site.mvc.SiteControllerBase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IndexController extends SiteControllerBase {

    @GetMapping("/")
    public String indexAction(HttpServletRequest request, @RequestParam Map<?, ?> param) throws Exception {
        return ApiUtil.echoResult(0, null, param);
    }

}
