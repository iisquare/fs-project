package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.oa.entity.Workflow;
import com.iisquare.fs.web.oa.storage.FormStorage;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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
    @Autowired
    private TaskService taskService;

    public Map<String, Object> form(Workflow workflow) {
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1101, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1102, "当前流程暂未关联表单", workflow);
        ObjectNode deployment = workflow.getDeploymentInfo();
        if (null == deployment) return ApiUtil.result(1301, "当前流程暂未部署", workflow);
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.get("id").asText()).singleResult();
        if (null == definition) return ApiUtil.result(1301, "当前流程定义异常", workflow);

        FlowElement element = repositoryService.getBpmnModel(definition.getId()).getMainProcess().getInitialFlowElement();
        List<ExtensionElement> authorities = element.getExtensionElements().get("authority");
        if (null == authorities || 1 != authorities.size()) return ApiUtil.result(1302, "节点权限配置异常", workflow);
        JsonNode authority = DPUtil.parseJSON(authorities.get(0).getElementText());
        if (null == authority || !authority.isObject()) return ApiUtil.result(1303, "节点权限解析异常", authority);

        ObjectNode result = DPUtil.objectNode();
        result.put("id", workflow.getId());
        result.put("name", workflow.getName());
        result.put("description", workflow.getDescription());
        result.put("formId", workflow.getFormId());
        result.replace("formInfo", workflow.getFormInfo());
        result.replace("authority", authority);
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> start(Workflow workflow, JsonNode form, JsonNode userInfo) {
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1101, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1102, "当前流程暂未关联表单", workflow);
        if (null == form || !form.isObject()) return ApiUtil.result(1201, "表单数据异常", form);
        ObjectNode deployment = workflow.getDeploymentInfo();
        if (null == deployment) return ApiUtil.result(1301, "当前流程暂未部署", workflow);
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.get("id").asText()).singleResult();
        if (null == definition) return ApiUtil.result(1301, "当前流程定义异常", workflow);

        FlowElement element = repositoryService.getBpmnModel(definition.getId()).getMainProcess().getInitialFlowElement();
        List<ExtensionElement> authorities = element.getExtensionElements().get("authority");
        if (null == authorities || 1 != authorities.size()) return ApiUtil.result(1302, "节点权限配置异常", workflow);
        JsonNode authority = DPUtil.parseJSON(authorities.get(0).getElementText());
        if (null == authority || !authority.isObject()) return ApiUtil.result(1303, "节点权限解析异常", authority);

        Map<String, Object> result = formService.validate(frame.at("/widgets"), form, authority, null, null);
        if (0 != (int) result.get("code")) return result;
        ObjectNode data = (ObjectNode) result.get("data");
        data = formService.save(frame, data, userInfo.get("uid").asInt());

        Map<String, Object> variables = DPUtil.convertJSON(data, Map.class); // TODO:生成流程变量
        identityService.setAuthenticatedUserId(String.valueOf(userInfo.get("uid").asInt()));
        ProcessInstance instance = runtimeService.startProcessInstanceById(definition.getId(), data.get(FormStorage.FIELD_ID).asText(), variables);

        if (null == instance) return ApiUtil.result(1302, "流程启动失败", workflow);
        // TODO:表单记录关联流程
        return ApiUtil.result(0, null, workflowService.processInstance2node(instance));
    }

}
