package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.ClientVersionService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 智能体客户端安装包版本管理。
 */
@RestController
@RequestMapping("/clientVersion")
public class ClientVersionController extends PermitControllerBase {

    @Autowired
    ClientVersionService clientVersionService;

    @RequestMapping("/list")
    @Permission("clientVersion:")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = clientVersionService.search(param, DPUtil.buildMap(
                "withUserInfo", true,
                "withStatusText", true,
                "withPlatformText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/info")
    @Permission("clientVersion:")
    public String infoAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(clientVersionService.detail(DPUtil.parseInt(param.get("id"))));
    }

    @PostMapping("/upload")
    @Permission("clientVersion:add")
    public String uploadAction(@RequestPart("file") MultipartFile file,
            @RequestParam Map<String, Object> param, HttpServletRequest request) {
        return ApiUtil.echoResult(clientVersionService.upload(file, param, request));
    }

    @RequestMapping("/save")
    @Permission("clientVersion:modify")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(clientVersionService.save(param, request));
    }

    @RequestMapping("/delete")
    @Permission("clientVersion:delete")
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(clientVersionService.remove(ids));
    }

    @GetMapping("/download")
    @Permission("clientVersion:")
    public String downloadAction(@RequestParam Map<String, Object> param,
            HttpServletResponse response) throws Exception {
        Map<String, Object> result = clientVersionService.download(param);
        if (ApiUtil.failed(result)) return ApiUtil.echoResult(result);
        return redirect(response, ApiUtil.data(result, JsonNode.class).asText());
    }

    @RequestMapping("/config")
    @Permission("clientVersion:")
    public String configAction(ModelMap model) {
        model.put("sorts", clientVersionService.sorts());
        model.put("status", clientVersionService.status());
        model.put("platforms", clientVersionService.platforms());
        model.put("architectures", clientVersionService.architectures());
        model.put("channels", clientVersionService.channels());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/stats")
    @Permission("clientVersion:")
    public String statsAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(0, null, clientVersionService.stats(param));
    }

}
