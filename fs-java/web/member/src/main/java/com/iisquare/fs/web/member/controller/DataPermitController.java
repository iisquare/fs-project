package com.iisquare.fs.web.member.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.entity.DataPermit;
import com.iisquare.fs.web.member.service.DataPermitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dataPermit")
public class DataPermitController extends PermitControllerBase {

    @Autowired
    private DataPermitService dataPermitService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        int id = DPUtil.parseInt(param.get("id"));
        DataPermit info = dataPermitService.info(id);
        if (null == info) {
            return ApiUtil.echoResult(0, null, DPUtil.objectNode());
        }
        return ApiUtil.echoResult(0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = dataPermitService.search(param, DPUtil.buildMap(
                "withDataInfo", true, "withRoleInfo", true, "withStatusText", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = dataPermitService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = dataPermitService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", dataPermitService.status());
        return ApiUtil.echoResult(0, null, model);
    }

}
