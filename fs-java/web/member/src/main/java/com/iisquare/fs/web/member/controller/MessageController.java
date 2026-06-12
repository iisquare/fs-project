package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController extends PermitControllerBase {

    @Autowired
    MessageService messageService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = messageService.search(param, DPUtil.buildMap());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = messageService.delete(ids, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

}
