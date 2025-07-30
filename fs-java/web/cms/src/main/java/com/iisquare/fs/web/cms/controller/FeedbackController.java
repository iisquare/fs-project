package com.iisquare.fs.web.cms.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.cms.service.FeedbackService;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feedback")
public class FeedbackController extends PermitControllerBase {

    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private MemberRpc memberRpc;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = feedbackService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withReferInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/audit")
    @Permission
    public String auditAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = feedbackService.audit(param, rbacService.uid(request));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = feedbackService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", feedbackService.status("default"));
        model.put("auditTag", RpcUtil.data(memberRpc.post("/dictionary/available",
                DPUtil.buildMap("path", "feedback-tag", "formatArray", true)), false));
        return ApiUtil.echoResult(0, null, model);
    }

}
