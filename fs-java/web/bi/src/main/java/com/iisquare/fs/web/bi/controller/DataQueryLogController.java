package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.service.DataQueryLogService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataQueryLog")
public class DataQueryLogController extends PermitControllerBase {

    @Autowired
    DataQueryLogService dataQueryLogService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = dataQueryLogService.info(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = dataQueryLogService.search(param, DPUtil.buildMap("withUserInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Long> ids = DPUtil.parseLongList(param.get("ids"));
        return ApiUtil.echoResult(dataQueryLogService.remove(ids));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("types", dataQueryLogService.types());
        model.put("status", dataQueryLogService.status());
        model.put("sorts", dataQueryLogService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

}
