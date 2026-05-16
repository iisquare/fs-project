package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.lm.service.AgentService;
import com.iisquare.fs.web.lm.service.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/")
public class IndexController extends ControllerBase {

    @Autowired
    AgentService agentService;
    @Autowired
    GatewayService gatewayService;

    @RequestMapping("/v1/chat/completions")
    public SseEmitter completionAction(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode json = (ObjectNode) DPUtil.parseJSON(body);
        return gatewayService.completion(json, request, response);
    }

    @RequestMapping("/v1/models")
    public String modelAction(@RequestParam Map<?, ?> param, HttpServletRequest request) {
        String token = gatewayService.token(request);
        ObjectNode result = gatewayService.models(token);
        return DPUtil.stringify(result);
    }

    @RequestMapping("/v1/agents")
    public String agentAction(HttpServletRequest request) {
        ObjectNode result = agentService.listByIdentity(request, true);
        return ApiUtil.echoResult(0, null, DPUtil.arrayNode(result));
    }

}
