package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.bi.entity.Cluster;
import com.iisquare.fs.web.bi.service.ClusterService;
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
@RequestMapping("/cluster")
public class ClusterController extends PermitControllerBase {

    @Autowired
    ClusterService clusterService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Cluster info = clusterService.info(id);
        if (null == info) return ApiUtil.echoResult(404, null, info);
        ObjectNode node = (ObjectNode) DPUtil.toJSON(info);
        node.replace("content", DPUtil.parseJSON(node.at("/content").asText(), k -> DPUtil.objectNode()));
        return ApiUtil.echoResult(0, null, node);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = clusterService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(clusterService.save(param, request));
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(clusterService.delete(ids, request));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", clusterService.status());
        model.put("engines", clusterService.engines());
        model.put("modes", clusterService.modes());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/options")
    @Permission("")
    public String optionsAction(@RequestBody Map<?, ?> param) {
        String engine = DPUtil.trim(DPUtil.parseString(param.get("engine")));
        return ApiUtil.echoResult(0, null, clusterService.options(engine));
    }

    @RequestMapping("/test")
    @Permission("")
    public String testAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(clusterService.test(param));
    }

}
