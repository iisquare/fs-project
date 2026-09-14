package com.iisquare.fs.web.kg.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.kg.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ontology")
public class OntologyController extends PermitControllerBase {

    @Autowired
    OntologyService ontologyService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = ontologyService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/model")
    @Permission("")
    public String modelAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = ontologyService.model(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = ontologyService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        try {
            Map<String, Object> result = ontologyService.save(param, request);
            return ApiUtil.echoResult(result);
        } catch (DataIntegrityViolationException e) {
            return ApiUtil.echoResult(500, "本体保存失败：定义存在重复或冲突，请检查实体、关系与字段名称", null);
        } catch (Exception e) {
            return ApiUtil.echoResult(500, "本体保存失败：" + e.getMessage(), null);
        }
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = ontologyService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", ontologyService.status());
        model.put("sorts", ontologyService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

}
