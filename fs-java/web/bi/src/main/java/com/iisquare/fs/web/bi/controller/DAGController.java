package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.web.bi.entity.Job;
import com.iisquare.fs.web.bi.service.DAGService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Map;

@RestController
@RequestMapping("/dag")
public class DAGController extends PermitControllerBase {

    @Autowired
    DAGService dagService;
    @Autowired
    Environment environment;

    @RequestMapping("/config")
    @Permission("bi:diagram:")
    public String configAction(ModelMap model) {
        model.put("clsTypes", DAGCore.simple(DAGCore.clsTypes, "name"));
        model.put("timezones", DAGCore.simple(DAGCore.timezones, "name"));
        model.put("locales", DAGCore.simple(DAGCore.locales, "name"));
        model.put("jdbcDrivers", DAGCore.simple(DAGCore.jdbcDrivers, "name"));
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping(value = "/diagram", params = "execution=true")
    public String executionDiagramAction(@RequestParam Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        if (!environment.acceptsProfiles(Profiles.of("dev", "test"))
                || !DPUtil.empty(request.getHeader("X-Forwarded-For"))
                || !DPUtil.empty(request.getHeader("X-Real-IP"))
                || !isLoopback(request.getRemoteAddr())) {
            return ApiUtil.echoResult(9403, "执行配置仅允许开发环境本机访问", null);
        }
        return ApiUtil.echoResult(dagService.executionDiagram(id));
    }

    @RequestMapping("/diagram")
    @Permission("bi:diagram:")
    public String diagramAction(@RequestParam Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        ObjectNode frame = dagService.diagram(id, DPUtil.objectNode());
        return ApiUtil.echoResult(null == frame ? 404 : 0, null, frame);
    }

    @RequestMapping("/run")
    @Permission("bi:diagram:run")
    public String runAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        return ApiUtil.echoResult(dagService.run(id, param, request, Job.TRIGGER_MANUAL));
    }

    @RequestMapping("/debug")
    @Permission("bi:diagram:run")
    public String debugAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        return ApiUtil.echoResult(dagService.debug(id, param, request));
    }

    @RequestMapping("/columns")
    @Permission("bi:datasource:schema")
    public String columnsAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(dagService.columns(param));
    }

    @RequestMapping("/tables")
    @Permission("bi:datasource:schema")
    public String tablesAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(dagService.tables(param));
    }

    private boolean isLoopback(String address) {
        if (DPUtil.empty(address)) return false;
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

}
