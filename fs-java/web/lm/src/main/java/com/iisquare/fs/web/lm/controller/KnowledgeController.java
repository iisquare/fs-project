package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.service.AIService;
import com.iisquare.fs.web.lm.service.KnowledgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController extends PermitControllerBase {

    @Autowired
    AIService aiService;
    @Autowired
    KnowledgeService knowledgeService;

    @RequestMapping("/embedding")
    @Permission({"add", "modify"})
    public String embeddingAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = knowledgeService.embedding(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        Map<String, Object> result = knowledgeService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = knowledgeService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withRoles", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = knowledgeService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = knowledgeService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", knowledgeService.status());
        model.put("recallTypes", knowledgeService.recallTypes());
        model.put("recallScopes", knowledgeService.recallScopes());
        Map<String, Object> result = aiService.models();
        model.put("models", ApiUtil.failed(result) ? DPUtil.arrayNode() : ApiUtil.data(result, ObjectNode.class).at("/data"));
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/recall")
    @Permission("")
    public String recallAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = knowledgeService.recall(param);
        return ApiUtil.echoResult(result);
    }

}
