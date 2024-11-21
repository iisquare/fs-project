package com.iisquare.fs.web.member.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.service.RbacService;
import com.iisquare.fs.web.member.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resource")
public class ResourceController extends PermitControllerBase {

    @Autowired
    private ResourceService resourceService;
    @Autowired
    private RbacService rbacService;

    @RequestMapping("/tree")
    @Permission("")
    public String treeAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(0, null, resourceService.tree(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true
        )));
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = resourceService.search(param, DPUtil.buildMap(
                "withApplicationInfo", true, "withUserInfo", true, "withStatusText", true, "withParentInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = resourceService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = resourceService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", resourceService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
