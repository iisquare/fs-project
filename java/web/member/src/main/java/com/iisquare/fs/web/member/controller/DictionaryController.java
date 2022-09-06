package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.service.DictionaryService;
import com.iisquare.fs.web.member.service.RbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dictionary")
public class DictionaryController extends PermitControllerBase {

    @Autowired
    private RbacService rbacService;
    @Autowired
    private DictionaryService dictionaryService;

    @RequestMapping("/available")
    public String availableAction(@RequestBody Map<?, ?> param) {
        String path = DPUtil.trim(DPUtil.parseString(param.get("path")));
        boolean withChildren = !DPUtil.empty(param.get("withChildren"));
        ObjectNode available = dictionaryService.available(true);
        ObjectNode result = dictionaryService.findAvailable(available, path, withChildren);
        boolean formatArray = !DPUtil.empty(param.get("formatArray"));
        return ApiUtil.echoResult(0, null, formatArray ? DPUtil.arrayNode(result) : result);
    }

    @RequestMapping("/tree")
    @Permission("")
    public String treeAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(0, null, dictionaryService.tree(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true
        )));
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = dictionaryService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withParentInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = dictionaryService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = dictionaryService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", dictionaryService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
