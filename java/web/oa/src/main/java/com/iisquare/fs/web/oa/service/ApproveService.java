package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.oa.entity.Workflow;
import com.iisquare.fs.web.oa.storage.FormStorage;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ApproveService extends ServiceBase {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private FormService formService;
    @Autowired
    private IdentityService identityService;


    public Map<String, Object> start(Workflow workflow, JsonNode form, int uid) {
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1101, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1102, "当前流程暂未关联表单", workflow);
        if (null == form || !form.isObject()) return ApiUtil.result(1201, "表单数据异常", form);
        ObjectNode deployment = workflow.getDeploymentInfo();
        if (null == deployment) return ApiUtil.result(1301, "当前流程暂未部署", workflow);
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.get("id").asText()).singleResult();
        if (null == definition) return ApiUtil.result(1301, "当前流程定义异常", workflow);

        Map<String, Object> result = formService.validate(frame.at("/widgets"), form, null);
        if (0 != (int) result.get("code")) return result;
        ObjectNode data = (ObjectNode) result.get("data");
        data = formService.save(frame, data, uid);

        Map<String, Object> variables = DPUtil.convertJSON(data, Map.class);
        identityService.setAuthenticatedUserId(String.valueOf(uid));
        ProcessInstance instance = runtimeService.startProcessInstanceById(definition.getId(), data.get(FormStorage.FIELD_ID).asText(), variables);
        if (null == instance) return ApiUtil.result(1302, "流程启动失败", workflow);
        return ApiUtil.result(0, null, workflowService.processInstance2node(instance));
    }

}
