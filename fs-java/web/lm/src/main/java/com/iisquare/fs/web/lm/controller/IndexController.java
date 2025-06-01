package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.lm.service.AgentService;
import com.iisquare.fs.web.lm.service.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/")
public class IndexController extends ControllerBase {

    @Autowired
    AgentService agentService;
    @Autowired
    ProxyService proxyService;

    @RequestMapping("/v1/chat/completions")
    public SseEmitter completionAction(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode json = (ObjectNode) DPUtil.parseJSON(body);
        return proxyService.completion(json, request, response);
    }

    @RequestMapping("/v1/models")
    public String modelAction(@RequestParam Map<?, ?> param, HttpServletRequest request) {
        String token = proxyService.token(request);
        ObjectNode result = proxyService.models(token);
        return DPUtil.stringify(result);
    }

    @RequestMapping("/v1/agents")
    public String agentAction(HttpServletRequest request) {
        ObjectNode result = agentService.listByIdentity(request, true);
        return ApiUtil.echoResult(0, null, DPUtil.arrayNode(result));
    }

}
