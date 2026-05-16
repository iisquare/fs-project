package com.iisquare.fs.web.spider.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.spider.service.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tool")
public class ToolController extends ControllerBase {

    @Autowired
    ToolService toolService;

    @GetMapping("/state")
    public String stateAction() {
        Map<String, Object> result = toolService.state();
        return ApiUtil.echoResult(result);
    }

    @PostMapping("/parse")
    public String parseAction(@RequestBody String json) {
        Map<String, Object> result = toolService.parse(DPUtil.parseJSON(json));
        return ApiUtil.echoResult(result);
    }

}
