package com.iisquare.fs.web.browser.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.browser.BrowserNodeService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/node")
public class NodeController extends PermitControllerBase {

    @Autowired
    BrowserNodeService browserNodeService;

    @GetMapping("/state")
    public String stateAction() {
        ObjectNode state = browserNodeService.state();
        return ApiUtil.echoResult(0, null, state);
    }
}
