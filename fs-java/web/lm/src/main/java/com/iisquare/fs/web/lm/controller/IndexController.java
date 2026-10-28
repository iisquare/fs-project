package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.lm.service.GatewayService;
import com.iisquare.fs.web.lm.service.ToolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/")
public class IndexController extends ControllerBase {

    @Autowired
    GatewayService gatewayService;
    @Autowired
    ToolsService toolsService;

    @RequestMapping("/v1/models")
    public String modelAction(@RequestParam Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) {
        ObjectNode result = gatewayService.models(param, request, response);
        return DPUtil.stringify(result);
    }

    /**
     * 当前用户的积分概览：认证口径同模型调用（服务密钥 + 可选的用户会话），
     * 返回总消耗、剩余与各积分限流周期的用量、剩余和下次重置时间。
     */
    @RequestMapping("/v1/credit")
    public String creditAction(HttpServletRequest request, HttpServletResponse response) {
        return DPUtil.stringify(gatewayService.credit(request, response));
    }

    @RequestMapping("/v1/chat/completions")
    public SseEmitter chatCompletionsAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return gatewayService.completion(json, request, response);
    }

    @RequestMapping("/v1/responses")
    public SseEmitter responsesAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return gatewayService.completion(json, request, response);
    }

    @RequestMapping("/v1/messages")
    public SseEmitter messagesAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return gatewayService.completion(json, request, response);
    }

    @RequestMapping("/v1/embeddings")
    public String embeddingsAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) {
        return DPUtil.stringify(gatewayService.embedding(json, request, response));
    }

    @RequestMapping("/v1/rerank")
    public String rerankAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) {
        return DPUtil.stringify(gatewayService.rerank(json, request, response));
    }

    @RequestMapping("/v1/tools/search")
    public String searchAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) {
        return toolsService.search(json, request, response);
    }

    @RequestMapping("/v1/tools/fetch")
    public String fetchAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) {
        return toolsService.fetch(json, request, response);
    }

}
