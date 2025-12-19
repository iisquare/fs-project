package com.iisquare.fs.web.crawler.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/")
public class IndexController extends ControllerBase {

    public String indexAction(@RequestBody Map<String, Object> param) {
        ObjectNode info = DPUtil.objectNode();
        info.put("name", "crawler");
        info.put("version", "0.0.1");
        return DPUtil.stringify(info);
    }

}
