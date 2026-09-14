package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.agent.service.SkillVersionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.ModelMap;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skillVersion")
public class SkillVersionController extends PermitControllerBase {

    @Autowired
    SkillVersionService versionService;

    @RequestMapping("/list")
    @Permission("skill:")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = versionService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withSkillInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @PostMapping("/upload")
    @Permission("skill:add")
    public String uploadAction(@RequestPart("file") MultipartFile file,
            @RequestParam Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = versionService.upload(file, param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/save")
    @Permission("skill:modify")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = versionService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission("skill:delete")
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = versionService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @GetMapping("/download")
    @Permission("skill:")
    public String downloadAction(@RequestParam Map<String, Object> param, HttpServletResponse response) throws Exception {
        Map<String, Object> result = versionService.download(param);
        if (ApiUtil.failed(result)) return ApiUtil.echoResult(result);
        return redirect(response, ApiUtil.data(result, JsonNode.class).asText());
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("sorts", versionService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

}
