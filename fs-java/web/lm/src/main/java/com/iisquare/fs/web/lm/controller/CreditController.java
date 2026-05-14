package com.iisquare.fs.web.lm.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lm.entity.Credit;
import com.iisquare.fs.web.lm.service.CreditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/credit")
public class CreditController extends PermitControllerBase {

    @Autowired
    private CreditService creditService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        int uid = DPUtil.parseInt(param.get("uid"));
        Credit info = creditService.info(uid);
        if (null == info) {
            return ApiUtil.echoResult(0, null, DPUtil.objectNode());
        }
        return ApiUtil.echoResult(0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = creditService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withRates", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = creditService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> uids = DPUtil.parseIntList(param.get("uids"));
        if(uids.isEmpty()) {
            uids = DPUtil.parseIntList(param.get("ids"));
        }
        boolean result = creditService.remove(uids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", creditService.status());
        return ApiUtil.echoResult(0, null, model);
    }

}
