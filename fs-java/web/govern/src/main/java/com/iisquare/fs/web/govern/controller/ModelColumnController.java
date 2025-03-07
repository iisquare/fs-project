package com.iisquare.fs.web.govern.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.govern.service.ModelColumnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/modelColumn")
public class ModelColumnController extends PermitControllerBase {

    @Autowired
    private ModelColumnService columnService;

    @RequestMapping("/list")
    @Permission("model:")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = columnService.search(param, DPUtil.buildMap());
        return ApiUtil.echoResult(0, null, result);
    }

}
