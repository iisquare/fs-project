package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.lm.service.GatewayService;
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

    @RequestMapping("/v1/models")
    public String modelAction(@RequestParam Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) {
        ObjectNode result = gatewayService.models(param, request, response);
        return DPUtil.stringify(result);
    }

    @RequestMapping("/v1/chat/completions")
    public SseEmitter chatCompletionsAction(@RequestBody ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
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

}
