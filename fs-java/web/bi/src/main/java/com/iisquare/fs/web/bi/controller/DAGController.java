package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.web.bi.service.DAGService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dag")
public class DAGController extends PermitControllerBase {

    @Autowired
    private DAGService dagService;

    @RequestMapping("/config")
    public String configAction(ModelMap model) {
        model.put("clsTypes", DAGCore.simple(DAGCore.clsTypes, "name"));
        model.put("timezones", DAGCore.simple(DAGCore.timezones, "name"));
        model.put("locales", DAGCore.simple(DAGCore.locales, "name"));
        model.put("jdbcDrivers", DAGCore.simple(DAGCore.jdbcDrivers, "name"));
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/diagram")
    public String diagramAction(@RequestParam Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        ObjectNode frame = dagService.diagram(id, DPUtil.objectNode());
        return ApiUtil.echoResult(null == frame ? 404 : 0, null, frame);
    }

    @RequestMapping("/run")
    public String runAction(@RequestParam Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        return ApiUtil.echoResult(dagService.run(id));
    }

}
