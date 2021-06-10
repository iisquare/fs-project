package com.iisquare.fs.dag.web.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.dag.core.DagCore;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tool")
public class ToolController extends PermitControllerBase {

    @RequestMapping("/field")
    public String fieldAction(@RequestParam Map<String, Object> param) {
        param.put("types", DagCore.types);
        return ApiUtil.echoResult(0, null, param);
    }

}
