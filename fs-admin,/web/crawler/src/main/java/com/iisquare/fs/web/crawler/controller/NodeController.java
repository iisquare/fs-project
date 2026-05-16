package com.iisquare.fs.web.crawler.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.crawler.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/node")
public class NodeController extends PermitControllerBase {

    @Autowired
    private NodeService nodeService;

    @GetMapping("/state")
    public String stateAction(@RequestParam Map<String, String> param) {
        try {
            ObjectNode data = nodeService.state();
            return ApiUtil.echoResult(0, null, data);
        } catch (Exception e) {
            return ApiUtil.echoResult(1500, "获取节点状态异常", e.getMessage());
        }
    }

}
