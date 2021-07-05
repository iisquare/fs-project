package com.iisquare.fs.web.oa.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
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
        Map<String, Object> result = approveService.form(workflowId);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/submit")
    @Permission("workflow")
    public String submitAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Integer workflowId = ValidateUtil.filterInteger(param.get("workflowId"), true, 1, null, 0);
        JsonNode form = DPUtil.convertJSON(param.get("form"));
        JsonNode audit = DPUtil.convertJSON(param.get("audit"));
        boolean modeComplete = DPUtil.parseBoolean(param.get("modeComplete"));
        Map<String, Object> result = approveService.start(workflowId, form, audit, rbacService.currentInfo(request), modeComplete);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/searchCandidate")
    @Permission("workflow")
    public String candidateAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<?, ?> result = approveService.searchCandidate(param, DPUtil.buildMap("withUserInfo", true), approveService.identity());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/claim")
    @Permission("workflow")
    public String claimAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String taskId = DPUtil.trim(DPUtil.parseString(param.get("taskId")));
        Map<String, Object> result = approveService.claim(taskId, rbacService.currentInfo(request));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/revocation")
    @Permission("workflow")
    public String revocationAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        String taskId = DPUtil.trim(DPUtil.parseString(param.get("taskId")));
        String reason = DPUtil.trim(DPUtil.parseString(param.get("reason")));
        Map<String, Object> result = approveService.revocation(processInstanceId, taskId, reason, rbacService.currentInfo(request));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/searchAssignee")
    @Permission("workflow")
    public String assigneeAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<?, ?> result = approveService.searchAssignee(param, DPUtil.buildMap("withUserInfo", true), approveService.identity());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/transact")
    @Permission("workflow")
    public String transactAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String taskId = DPUtil.trim(DPUtil.parseString(param.get("taskId")));
        Map<String, Object> result = approveService.transact(taskId, rbacService.currentInfo(request));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/complete")
    @Permission("workflow")
    public String completeAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String taskId = DPUtil.trim(DPUtil.parseString(param.get("taskId")));
        JsonNode form = DPUtil.convertJSON(param.get("form"));
        JsonNode audit = DPUtil.convertJSON(param.get("audit"));
        boolean modeComplete = DPUtil.parseBoolean(param.get("modeComplete"));
        boolean modeReject = DPUtil.parseBoolean(param.get("modeReject"));
        Map<String, Object> result = approveService.complete(taskId, form, audit, rbacService.currentInfo(request), modeComplete, modeReject);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/searchHistory")
    @Permission("workflow")
    public String historyAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<?, ?> result = approveService.searchHistory(param, DPUtil.buildMap("withUserInfo", true), approveService.identity());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/process")
    @Permission("workflow")
    public String processAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        String processInstanceId = DPUtil.trim(DPUtil.parseString(param.get("processInstanceId")));
        String taskId = DPUtil.trim(DPUtil.parseString(param.get("taskId")));
        Map<String, Object> result = approveService.process(processInstanceId, taskId, rbacService.currentInfo(request), null);
        return ApiUtil.echoResult(result);
    }

}
