package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.service.KnowledgeChunkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledgeChunk")
public class KnowledgeChunkController extends PermitControllerBase {

    @Autowired
    private KnowledgeChunkService chunkService;

    @RequestMapping("/list")
    @Permission("knowledge:")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = chunkService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withKnowledgeInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"knowledge:add", "knowledge:modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = chunkService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission("knowledge:delete")
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = chunkService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("knowledge:")
    public String configAction(ModelMap model) {
        model.put("status", chunkService.status());
        return ApiUtil.echoResult(0, null, model);
    }

}
