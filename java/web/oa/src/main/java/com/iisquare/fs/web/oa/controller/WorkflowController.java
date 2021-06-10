package com.iisquare.fs.web.oa.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.oa.entity.Workflow;
import com.iisquare.fs.web.oa.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
public class WorkflowController extends PermitControllerBase {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/publish")
    public String publishAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Map<String, Object> result = workflowService.deployment(id, rbacService.uid(request));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/info")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        boolean withDeployment = !DPUtil.empty(param.get("withDeployment"));
        boolean withForm = !DPUtil.empty(param.get("withForm"));
        boolean withFormDetail = !DPUtil.empty(param.get("withFormDetail"));
        boolean withFormRemote = !DPUtil.empty(param.get("withFormRemote"));
        Workflow info = workflowService.info(id, withDeployment, withForm, withFormDetail, withFormRemote);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = workflowService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withDeploymentInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/searchDeployment")
    @Permission("searchDeployment")
    public String searchDeploymentAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = workflowService.searchDeployment(param, DPUtil.buildMap());
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/deleteDeployment")
    @Permission("deleteDeployment")
    public String deleteDeploymentAction(@RequestBody Map<?, ?> param) {
        String id = DPUtil.parseString(param.get("id"));
        boolean cascade = !DPUtil.empty(param.get("cascade"));
        workflowService.deleteDeployment(id, cascade);
        return ApiUtil.echoResult(0, null, id);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Integer formId = ValidateUtil.filterInteger(param.get("formId"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String description = DPUtil.parseString(param.get("description"));
        String content = DPUtil.parseString(param.get("content"));
        Workflow info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = workflowService.info(id, false, false, false, false);
            if(null == info) return ApiUtil.echoResult(404, null, id);
            if(param.containsKey("name")) {
                if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
                info.setName(name);
            }
            if(param.containsKey("sort")) info.setSort(sort);
            if(param.containsKey("status")) {
                if(!workflowService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
                info.setStatus(status);
            }
            if(param.containsKey("description")) info.setDescription(description);
            if(param.containsKey("formId")) info.setFormId(formId);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Workflow();
            if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
            info.setName(name);
            info.setSort(sort);
            if(!workflowService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
            info.setStatus(status);
            info.setDescription(description);
            info.setFormId(formId);
        }
        info.setContent(content);
        info = workflowService.save(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = workflowService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", workflowService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
