package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/chat")
public class ChatController extends PermitControllerBase {

    @Autowired
    ChatService chatService;

    @RequestMapping("/demo")
    @Permission
    public SseEmitter demoAction(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode json = (ObjectNode) DPUtil.parseJSON(body);
        return chatService.demo(json, request, response);
    }

    @RequestMapping("/compare")
    @Permission
    public SseEmitter compareAction(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode json = (ObjectNode) DPUtil.parseJSON(body);
        return chatService.compare(json, request, response);
    }

    @RequestMapping("/dialog")
    @Permission
    public SseEmitter dialogAction(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode json = (ObjectNode) DPUtil.parseJSON(body);
        return chatService.dialog(json, request, response);
    }

}
