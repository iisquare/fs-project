package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.admin.mvc.AdminControllerBase;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

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

    @RequestMapping("/body")
    public String bodyAction(@RequestBody String body, HttpServletResponse response) throws Exception {
        return displayText(response, body);
    }

    @RequestMapping("/form")
    public String formAction(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // System.out.println(request.getReader().readLine());
        // spring.servlet.multipart.enabled: false
        // fixed: request.getReader() -> getInputStream() has already been called for this request
        Enumeration<String> enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();
            displayText(response, String.format("%s: %s\r\n", name, request.getHeader(name)));
        }
        byte[] bytes = IOUtils.toByteArray(request.getInputStream());
        return displayText(response, new String(bytes, StandardCharsets.UTF_8));
    }

}
