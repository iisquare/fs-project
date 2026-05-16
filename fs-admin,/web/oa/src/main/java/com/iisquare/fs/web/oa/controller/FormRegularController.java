package com.iisquare.fs.web.oa.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.oa.entity.FormRegular;
import com.iisquare.fs.web.oa.service.FormRegularService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/formRegular")
public class FormRegularController extends PermitControllerBase {

    @Autowired
    private FormRegularService formRegularService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/test")
    public String testAction(@RequestBody Map<?, ?> param) {
        String regex = DPUtil.parseString(param.get("regex"));
        String content = DPUtil.parseString(param.get("content"));
        boolean result = DPUtil.isMatcher(regex, content);
        if (result) {
            return ApiUtil.echoResult(0, "校验通过", true);
        }
        return ApiUtil.echoResult(1000, "校验不通过", false);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        FormRegular info = formRegularService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/all")
    public String allAction(@RequestBody Map<?, ?> param) {
        ObjectNode result = formRegularService.all();
        return ApiUtil.echoResult(null == result ? 500 : 0, null, DPUtil.arrayNode(result));
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = formRegularService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = formRegularService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = formRegularService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", formRegularService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
