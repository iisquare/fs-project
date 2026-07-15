package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.entity.KnowledgeDocument;
import com.iisquare.fs.web.lm.service.KnowledgeDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledgeDocument")
public class KnowledgeDocumentController extends PermitControllerBase {

    @Autowired
    KnowledgeDocumentService documentService;

    @RequestMapping("/info")
    @Permission("knowledge:")
    public String infoAction(@RequestParam Map<?, ?> param) {
        int id = DPUtil.parseInt(param.get("id"));
        KnowledgeDocument info = documentService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("knowledge:")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = documentService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withKnowledgeInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @PostMapping("/upload")
    @Permission("knowledge:add")
    public String uploadAction(@RequestPart("file") MultipartFile file,
            @RequestParam Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = documentService.upload(file, param, request);
        return ApiUtil.echoResult(result);
    }

    @GetMapping("/download")
    @Permission("knowledge:")
    public String downloadAction(@RequestParam Map<String, Object> param, HttpServletResponse response) throws Exception {
        Map<String, Object> result = documentService.download(param);
        if (ApiUtil.failed(result)) return ApiUtil.echoResult(result);
        return redirect(response, ApiUtil.data(result, JsonNode.class).asText());
    }

    @RequestMapping("/save")
    @Permission("knowledge:modify")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = documentService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission("knowledge:delete")
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = documentService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("knowledge:")
    public String configAction(ModelMap model) {
        model.put("status", documentService.status());
        return ApiUtil.echoResult(0, null, model);
    }

}
