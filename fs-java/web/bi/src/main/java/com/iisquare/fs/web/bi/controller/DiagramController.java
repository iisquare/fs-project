package com.iisquare.fs.web.bi.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.web.bi.entity.Diagram;
import com.iisquare.fs.web.bi.service.DiagramService;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/diagram")
public class DiagramController extends PermitControllerBase {

    @Autowired
    DiagramService diagramService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Diagram info = diagramService.info(id);
        if (null == info) return ApiUtil.echoResult(404, null, info);
        ObjectNode node = (ObjectNode) DPUtil.toJSON(info);
        node.replace("content", DPUtil.parseJSON(node.at("/content").asText(), k -> DPUtil.objectNode()));
        node.replace("notify", DPUtil.parseJSON(node.at("/notify").asText(), k -> DPUtil.objectNode()));
        return ApiUtil.echoResult(0, null, node);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = diagramService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withEngineText", true, "withModelText", true,
            "withClusterName", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = diagramService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(diagramService.delete(ids, rbacService.uid(request)));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", diagramService.status("default"));
        model.put("engines", diagramService.engines());
        model.put("models", diagramService.models());
        return ApiUtil.echoResult(0, null, model);
    }

}
