package com.iisquare.fs.web.oa.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.oa.entity.Workflow;
import com.iisquare.fs.web.oa.service.ApproveService;
import com.iisquare.fs.web.oa.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/approve")
public class ApproveController extends PermitControllerBase {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private ApproveService approveService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/workflow")
    @Permission("workflow")
    public String workflowAction(@RequestBody Map<String, Object> param) {
        param.put("status", 1);
        Map<?, ?> result = workflowService.search(param, DPUtil.buildMap("withDeploymentInfo", true, "withBrief", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/form")
    @Permission("workflow")
    public String formAction(@RequestBody Map<String, Object> param) {
        Integer workflowId = ValidateUtil.filterInteger(param.get("workflowId"), true, 1, null, 0);
        Workflow workflow = workflowService.info(workflowId, true, true, false, true);
        Map<String, Object> result = approveService.form(workflow);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/submit")
    @Permission("workflow")
    public String submitAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Integer workflowId = ValidateUtil.filterInteger(param.get("workflowId"), true, 1, null, 0);
        Workflow workflow = workflowService.info(workflowId, true, true, true, true);
        JsonNode form = DPUtil.convertJSON(param.get("form"));
        Map<String, Object> result = approveService.start(workflow, form, rbacService.currentInfo(request));
        return ApiUtil.echoResult(result);
    }

}
