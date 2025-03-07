package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.service.SensitiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sensitive")
public class SensitiveController extends PermitControllerBase {

    @Autowired
    private SensitiveService sensitiveService;

    @RequestMapping("/window")
    @Permission("")
    public String windowAction(@RequestBody Map<String, Object> param) {
        int size = sensitiveService.window();
        String sentence = DPUtil.parseString(param.get("sentence"));
        String window = sensitiveService.window(sentence, size);
        ObjectNode result = DPUtil.objectNode();
        result.put("size", size);
        result.put("window", window);
        result.put("sentence", sentence);
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/check")
    @Permission("")
    public String checkAction(@RequestBody Map<String, Object> param) {
        String sentence = DPUtil.parseString(param.get("sentence"));
        List<String> result = sensitiveService.check(sentence);
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = sensitiveService.search(param, DPUtil.buildMap("withUserInfo", true, "withStatusText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = sensitiveService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = sensitiveService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", sensitiveService.status());
        JsonNode risk = sensitiveService.risk();
        if (null == risk) {
            return ApiUtil.echoResult(1001, "获取字典失败", null);
        }
        model.put("risk", risk);
        return ApiUtil.echoResult(0, null, model);
    }

}
