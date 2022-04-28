package com.iisquare.fs.web.govern.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.govern.service.ModelColumnService;
import com.iisquare.fs.web.govern.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/model")
public class ModelController extends PermitControllerBase {

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelColumnService columnService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = modelService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = modelService.search(param, DPUtil.buildMap(
                "withUserInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = modelService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = modelService.delete(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("types", modelService.types());
        model.put("columnTypes", columnService.types());
        return ApiUtil.echoResult(0, null, model);
    }

}
