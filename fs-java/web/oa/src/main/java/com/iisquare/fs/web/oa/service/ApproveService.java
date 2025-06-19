package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import com.iisquare.fs.web.oa.entity.Workflow;
import com.iisquare.fs.web.oa.storage.FormStorage;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 在获取流程节点时，若通过singleResult取单值，请务必确保筛选条件不会命中多条记录。
 * auditTaskId为上级审批节点的任务ID，用于comment评审意见权限过滤。
 */
@Service
public class ApproveService extends ServiceBase {

    @Autowired
    private RepositoryService repositoryService; // 流程仓库服务，管理流程仓库，比如部署、删除、读取流程资源。
    @Autowired
    private IdentityService identityService; // 身份服务，管理用户、组及其关系。
    @Autowired
    private RuntimeService runtimeService; // 运行服务，处理所有正在运行态的流程实例、任务等。
    @Autowired
    private TaskService taskService; // 任务服务，管理（签收、办理、指派等）、查询任务。
    @Autowired
    private HistoryService historyService; // 历史服务，管理所有历史数据。

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private FormService formService; // 自定义表单服务（非flowable包），读取和流程、任务相关的表单数据。
    @Autowired
    private MemberRpc memberRpc;
    @Autowired
    private DefaultRbacService rbacService;

    public String businessKey(ObjectNode frame, ObjectNode form) {
        return "fs-" + form.get(FormStorage.FIELD_ID).asText();
    }

    /**
     * 查找申报节点，唯一连接StartEvent的UserTask
     */
    public UserTask genesis(String definitionId) {
        if (DPUtil.empty(definitionId)) return null;
        FlowElement element = repositoryService.getBpmnModel(definitionId).getMainProcess().getInitialFlowElement();
        if (!(element instanceof StartEvent)) return null;
        StartEvent startEvent = (StartEvent) element;
        if (startEvent.getOutgoingFlows().size() != 1) return null;
        SequenceFlow sequenceFlow = startEvent.getOutgoingFlows().get(0);
        element = sequenceFlow.getTargetFlowElement();
        if (null == element || !(element instanceof UserTask)) return null;
        return (UserTask) element;
    }

    /**
     * 获取申报表单信息
     */
    public Map<String, Object> form(Integer workflowId) {
        Workflow workflow = workflowService.info(workflowId, true, true, false, true);
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1101, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1102, "当前流程暂未关联表单", workflow);
        ObjectNode deployment = workflow.getDeploymentInfo();
        if (DPUtil.empty(deployment.at("/id").asText())) return ApiUtil.result(1301, "当前流程暂未部署", workflow);
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.get("id").asText()).singleResult();
        if (null == definition) return ApiUtil.result(1301, "当前流程定义异常", workflow);

        FlowElement element = genesis(definition.getId());
        if (null == element) return ApiUtil.result(1302, "申报节点获取异常", definition.getId());
        List<ExtensionElement> authorities = element.getExtensionElements().get("authority");
        if (null == authorities || 1 != authorities.size()) return ApiUtil.result(1303, "节点权限配置异常", workflow);
        JsonNode authority = DPUtil.parseJSON(authorities.get(0).getElementText());
        if (null == authority || !authority.isObject()) return ApiUtil.result(1304, "节点权限解析异常", authority);

        ObjectNode result = DPUtil.objectNode();
        result.put("id", workflow.getId());
        result.put("name", workflow.getName());
        result.put("content", workflow.getContent());
        result.put("description", workflow.getDescription());
        result.put("formId", workflow.getFormId());
        result.replace("formInfo", workflow.getFormInfo());
        result.replace("authority", authority);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 格式化审批备注
     */
    public ObjectNode audit(JsonNode audit) {
        if (null == audit || !audit.isObject()) audit = DPUtil.objectNode();
        ObjectNode result = DPUtil.objectNode();
        result.put("local", audit.at("/local").asBoolean(false));
        result.put("message", audit.at("/message").asText(""));
        return result;
    }

    /**
     * 提交申报单据，启动流程
     */
    public Map<String, Object> start(Integer workflowId, JsonNode form, JsonNode audit, JsonNode userInfo, boolean modeComplete) {
        Workflow workflow = workflowService.info(workflowId, true, true, true, true);
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1101, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1102, "当前流程暂未关联表单", workflow);
        if (null == form || !form.isObject()) return ApiUtil.result(1201, "表单数据异常", form);
        ObjectNode deployment = workflow.getDeploymentInfo();
        if (null == deployment) return ApiUtil.result(1301, "当前流程暂未部署", workflow);
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.get("id").asText()).singleResult();
        if (null == definition) return ApiUtil.result(1301, "当前流程定义异常", workflow);

        FlowElement element = genesis(definition.getId());
        if (null == element) return ApiUtil.result(1302, "申报节点获取异常", definition.getId());
        List<ExtensionElement> authorities = element.getExtensionElements().get("authority");
        if (null == authorities || 1 != authorities.size()) return ApiUtil.result(1303, "节点权限配置异常", workflow);
        JsonNode authority = DPUtil.parseJSON(authorities.get(0).getElementText());
        if (null == authority || !authority.isObject()) return ApiUtil.result(1304, "节点权限解析异常", authority);

        Map<String, Object> result = formService.validate(frame.at("/widgets"), form, authority, null, null);
        if (0 != (int) result.get("code")) return result;
        ObjectNode data = (ObjectNode) result.get("data");
        data = formService.save(frame, data, userInfo.get("uid").asInt());

        // 启动流程
        String submitter = String.valueOf(userInfo.get("uid").asInt());
        ObjectNode variables = formService.filtration(frame.at("/widgets"), data, authority, "variable", false);
        Map<String, Object> processVariables = new LinkedHashMap<>(); // 流程变量
        processVariables.put("submitter", submitter); // 流程发起人
        processVariables.put("workflowId", workflow.getId()); // 流程模型
        processVariables.put("formStorageId", data.get(FormStorage.FIELD_ID).asText()); // 表单存储
        processVariables.put("genesisElementId", element.getId()); // 申报节点
        identityService.setAuthenticatedUserId(submitter);
        ProcessInstance instance = runtimeService.startProcessInstanceById(definition.getId(), businessKey(frame, data), processVariables);
        if (null == instance) return ApiUtil.result(1302, "流程启动失败", workflow);

        // 表单记录关联流程
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).taskDefinitionKey(element.getId()).active().singleResult();
        data.put("bpmWorkflowId", workflow.getId());
        data.put("bpmInstanceId", instance.getId());
        data.put("bpmStartUserId", instance.getStartUserId());
        formService.save(frame, data, userInfo.get("uid").asInt());
        taskService.addComment(task.getId(), task.getProcessInstanceId(), DPUtil.stringify(audit(audit)));

        // 执行申报节点
        taskService.setAssignee(task.getId(), submitter);
        if (modeComplete) { // 提交申报记录
            Map<String, Object> taskVariables = DPUtil.toJSON(variables, Map.class);
            taskService.complete(task.getId(), taskVariables, true); // 局部变量
            List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).active().list();
            for (Task activeTask : activeTasks) { // 记录任务来源
                taskService.setVariableLocal(activeTask.getId(), "auditTaskId", task.getId());
            }
        }
        return ApiUtil.result(0, null, workflowService.processInstance2node(instance));
    }

    /**
     * 获取当前登录用户信息
     */
    public JsonNode identity() {
        ObjectNode identity = (ObjectNode) RpcUtil.data(memberRpc.post("/rbac/identity", DPUtil.buildMap()), false);
        if (!identity.has("id")) return identity;
        ArrayNode candidate = identity.putArray("candidate");
        candidate.add(identity.get("id").asText()); // 直接参与人
        candidate.add("user-" + identity.get("id").asInt()); // 候选用户
        Iterator<JsonNode> iterator = identity.get("roles").iterator();
        while (iterator.hasNext()) {
            JsonNode role = iterator.next();
            candidate.add("role-" + role.get("id").asInt()); // 候选角色
        }
        return identity;
    }

    /**
     * 待签任务，候选或指派给当前登录用户的任务
     */
    public Map<String, Object> searchCandidate(Map<?, ?> param, Map<?, ?> config, JsonNode identity) {
        if (!identity.has("id")) return ApiUtil.result(1403, "获取当前用户信息失败", identity);
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        TaskQuery query = taskService.createTaskQuery()
                .taskCandidateGroupIn(DPUtil.toJSON(identity.get("candidate"), List.class))
                .orderByTaskPriority().desc().orderByTaskCreateTime().asc();
        String deploymentId = DPUtil.trim(DPUtil.parseString(param.get("deploymentId")));
        if(!DPUtil.empty(deploymentId)) {
            query.deploymentId(deploymentId);
        }
        String submitter = DPUtil.trim(DPUtil.parseString(param.get("submitter")));
        if(!DPUtil.empty(submitter)) {
            query.processVariableValueEquals("submitter", submitter);
        }
        long count = query.count();
        ArrayNode rows = workflowService.task2node(count > 0 ? query.listPage((page - 1) * pageSize, pageSize) : new ArrayList<>());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", count);
        result.put("rows", fillProcessInstance(workflowService.fillDeployment(rows), config));
        return result;
    }

    public ArrayNode fillProcessInstance(ArrayNode array, Map<?, ?> config) {
        if (null == array || array.size() < 1) return array;
        Set<String> processInstanceIds = DPUtil.values(array, String.class, "processInstanceId");
        ArrayNode rows = workflowService.processInstance2node(runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds).list());
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo("Id", "Info", rows, "startUserId");
        }
        ObjectNode instances = DPUtil.json2object(rows, "id");
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            JsonNode instance = instances.get(node.get("processInstanceId").asText());
            node.replace("processInstanceInfo", instance);
        }
        return array;
    }

    /**
     * 签收任务
     */
    public Map<String, Object> claim(String taskId, JsonNode userInfo) {
        if (DPUtil.empty(taskId)) return ApiUtil.result(1001, "参数异常", taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (null == task) {
            return ApiUtil.result(1002, "任务不存在", taskId);
        }
        if (null != task.getAssignee()) {
            if (task.getAssignee().equals(userInfo.get("uid").asText())) {
                return ApiUtil.result(1003, "任务已签收", workflowService.task2node(task));
            } else {
                return ApiUtil.result(1004, "任务已分派", workflowService.task2node(task));
            }
        }
        if (task.isSuspended()) return ApiUtil.result(1401, "任务已挂起", workflowService.task2node(task));
        try {
            taskService.claim(taskId, userInfo.get("uid").asText());
        } catch (Exception e) {
            return ApiUtil.result(1500, "任务签收失败", e.getMessage());
        }
        return ApiUtil.result(0, "任务签收成功", taskId);
    }

    /**
     * 撤销任务
     */
    public Map<String, Object> revocation(String processInstanceId, String taskId, String reason, JsonNode userInfo) {
        if (DPUtil.empty(processInstanceId) || DPUtil.empty(taskId)) {
            return ApiUtil.result(1001, "流程参数异常", processInstanceId);
        }
        if (DPUtil.empty(reason)) return ApiUtil.result(1002, "请填写撤销原因", processInstanceId);
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (null == instance || instance.isEnded()) return ApiUtil.result(1101, "流程实例不可用", processInstanceId);
        if (!userInfo.get("uid").asText().equals(instance.getStartUserId())) {
            return ApiUtil.result(1102, "当前用户不具备撤销权限", workflowService.processInstance2node(instance));
        }
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).taskId(taskId).active().singleResult();
        if (null == task) return ApiUtil.result(1201, "当前流程无有效活动任务", workflowService.processInstance2node(instance));
        if (!userInfo.get("uid").asText().equals(task.getAssignee())) {
            return ApiUtil.result(1202, "当前流程暂不可撤销", workflowService.task2node(task));
        }
        if (task.isSuspended()) return ApiUtil.result(1401, "任务已挂起", workflowService.task2node(task));
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        return ApiUtil.result(0, "撤销成功", workflowService.processInstance2node(instance));
    }

    /**
     * 待办任务，候选或指派给当前登录用户的任务
     */
    public Map<String, Object> searchAssignee(Map<?, ?> param, Map<?, ?> config, JsonNode identity) {
        if (!identity.has("id")) return ApiUtil.result(1403, "获取当前用户信息失败", identity);
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        TaskQuery query = taskService.createTaskQuery()
                .taskAssignee(identity.get("id").asText())
                .orderByTaskPriority().desc().orderByTaskCreateTime().asc();
        String deploymentId = DPUtil.trim(DPUtil.parseString(param.get("deploymentId")));
        if(!DPUtil.empty(deploymentId)) {
            query.deploymentId(deploymentId);
        }
        String submitter = DPUtil.trim(DPUtil.parseString(param.get("submitter")));
        if(!DPUtil.empty(submitter)) {
            query.processVariableValueEquals("submitter", submitter);
        }
        long count = query.count();
        ArrayNode rows = workflowService.task2node(count > 0 ? query.listPage((page - 1) * pageSize, pageSize) : new ArrayList<>());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", count);
        result.put("rows", fillProcessInstance(workflowService.fillDeployment(rows), config));
        return result;
    }

    /**
     * 获取待处理任务信息
     */
    public Map<String, Object> transact(String taskId, JsonNode userInfo) {
        if (DPUtil.empty(taskId)) return ApiUtil.result(1001, "参数异常", taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (null == task) return ApiUtil.result(1002, "任务不存在", taskId);
        String uid = userInfo.get("uid").asText();
        if (null == task.getAssignee() || !task.getAssignee().equals(uid)) {
            return ApiUtil.result(1003, "当前任务不可办理", workflowService.task2node(task));
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        if (null == instance) return ApiUtil.result(1003, "流程实例不存在", workflowService.task2node(task));
        Map<String, Object> variables = taskService.getVariables(task.getId());

        // 获取流程模型
        Workflow workflow = workflowService.info(DPUtil.parseInt(variables.get("workflowId")), false, true, true, true);
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1201, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1202, "当前流程关联表单异常", workflow);

        // 流程节点权限
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId()).singleResult();
        if (null == definition) return ApiUtil.result(1401, "当前流程定义异常", workflowService.task2node(task));
        FlowElement element = repositoryService.getBpmnModel(definition.getId()).getFlowElement(task.getTaskDefinitionKey());
        List<ExtensionElement> authorities = element.getExtensionElements().get("authority");
        if (null == authorities || 1 != authorities.size()) {
            return ApiUtil.result(1402, "节点权限配置异常", workflowService.processDefinition2node(definition));
        }
        JsonNode authority = DPUtil.parseJSON(authorities.get(0).getElementText());
        if (null == authority || !authority.isObject()) return ApiUtil.result(1403, "节点权限解析异常", authority);

        // 获取表单数据
        ObjectNode form = formService.info(frame, DPUtil.parseString(variables.get("formStorageId")));
        if (null == form || !form.isObject()) return ApiUtil.result(1601, "表单数据异常", variables);
        form = formService.filtration(frame.at("/widgets"), form, authority, "viewable", true);

        // 获取审批信息
        List<Comment> comments = taskService.getProcessInstanceComments(task.getProcessInstanceId());
        List<String> keepTaskIds = new ArrayList<>();
        keepTaskIds.add( DPUtil.parseString(variables.get("auditTaskId")));

        // 流程历史信息
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(instance.getId()).list();
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(instance.getId()).list();
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            if (null != keepTaskIds && uid.equals(historicActivityInstance.getAssignee())) keepTaskIds.add(historicActivityInstance.getTaskId());
        }

        ObjectNode result = DPUtil.objectNode();
        result.put("id", workflow.getId());
        result.put("name", workflow.getName());
        result.put("content", workflow.getContent());
        result.put("description", workflow.getDescription());
        result.replace("form", form);
        result.put("formId", workflow.getFormId());
        result.replace("formInfo", workflow.getFormInfo());
        result.replace("authority", authority);
        result.put("taskId", task.getId());
        result.replace("taskInfo", workflowService.task2node(task));
        result.replace("comments", audit(comments, keepTaskIds));
        result.replace("processInstanceInfo", workflowService.processInstance2node(instance));
        result.replace("historicTaskInstances", workflowService.historicTaskInstance2node(historicTaskInstances));
        result.replace("historicActivityInstances", workflowService.historicActivityInstance2node(historicActivityInstances));
        if (!fillUserInfo(result, "processInstanceInfo")) {
            return ApiUtil.result(1901, "获取用户信息失败", result);
        }
        return ApiUtil.result(0, null, result);
    }

    public ObjectNode audit(List<Comment> comments, List<String> keepTaskIds) {
        ObjectNode result = DPUtil.objectNode();
        if (null == comments) return result;
        for (Comment comment : comments) {
            String taskId = comment.getTaskId();
            ArrayNode array = result.has(taskId) ? (ArrayNode) result.get(taskId) : result.putArray(taskId);
            ObjectNode node = workflowService.comment2node(comment);
            ObjectNode audit = audit(DPUtil.parseJSON(comment.getFullMessage()));
            node.replace("audit", audit);
            if (!audit.get("local").asBoolean() || null == keepTaskIds || keepTaskIds.contains(taskId)) {
                array.add(node); // 不展示局部无权限的审批备注
            }
        }
        return result;
    }

    public boolean fillUserInfo(ObjectNode workflow, String fieldInstance) {
        ObjectNode instance = (ObjectNode) workflow.get(fieldInstance);
        Set<String> ids = new HashSet<>();
        ids.add(instance.get("startUserId").asText());
        ids.addAll(DPUtil.values((ArrayNode) workflow.get("historicTaskInstances"), String.class, "assignee"));
        JsonNode userInfos = RpcUtil.data(memberRpc.post("/rbac/listByIds", DPUtil.buildMap("ids", ids)), false);
        if (null == userInfos) return false;
        if(userInfos.size() < 1) return true;
        instance.put("startUserName", userInfos.at("/" + instance.get("startUserId").asText() + "/name").asText());
        Iterator<JsonNode> iterator = workflow.get("historicTaskInstances").iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            String assignee = node.get("assignee").asText();
            if (!userInfos.has(assignee)) continue;
            node.put("assigneeName", userInfos.get(assignee).get("name").asText());
        }
        return true;
    }

    /**
     * 驳回任务
     */
    public Map<String, Object> reject(String processInstanceId, String taskId, JsonNode audit, JsonNode userInfo) {
        // 流程校验
        if (DPUtil.empty(processInstanceId)) return ApiUtil.result(1901, "流程实例参数异常", processInstanceId);
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (DPUtil.empty(instance)) return ApiUtil.result(1902, "流程实例不存在", processInstanceId);
        // 上下文参数校验
        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();
        Map<String, Object> variables = variables(historicVariableInstances);
        String submitter = DPUtil.parseString(variables.get("submitter"));
        if (DPUtil.empty(submitter)) return ApiUtil.result(1903, "获取流程发起人信息异常", variables);
        String genesisElementId = DPUtil.parseString(variables.get("genesisElementId"));
        if (DPUtil.empty(genesisElementId)) return ApiUtil.result(1904, "获取申报节点信息异常", variables);
        // 历史流程信息
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(instance.getId()).list();
        List<HistoricActivityInstance> activityInstances = new ArrayList<>(); // 当前活动节点
        HistoricActivityInstance genesisActivityInstance = null; // 申报节点
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            if (null == historicActivityInstance.getEndTime()) {
                activityInstances.add(historicActivityInstance);
            }
            if (null == genesisActivityInstance && genesisElementId.equals(historicActivityInstance.getActivityId())) {
                genesisActivityInstance = historicActivityInstance;
            }
        }
        ArrayList<String> activityIds = new ArrayList<>(DPUtil.values(activityInstances, String.class, "activityId"));
        if (activityIds.size() < 1) {
            return ApiUtil.result(1905, "当前流程无有效活动节点", workflowService.processInstance2node(instance));
        }
        if (null == genesisActivityInstance) {
            return ApiUtil.result(1906, "查找申报节点失败", workflowService.processInstance2node(instance));
        }
        Set<String> taskIds = DPUtil.values(activityInstances, String.class, "taskId");
        if (DPUtil.empty(taskId)) { // 管理操作
            if (taskIds.size() != 1) return ApiUtil.result(1907, "当前流程存在多个活跃任务，请联系节点负责人驳回！", taskIds);
            taskId = taskIds.iterator().next();
        } else {
            if (!taskIds.contains(taskId)) {
                return ApiUtil.result(1908, "当前任务不在有效活动节点中", taskId);
            }
        }
        identityService.setAuthenticatedUserId(userInfo.get("uid").asText()); // 记录操作人员信息
        this.deleteComments(taskId); // 清除审批备注历史记录
        taskService.addComment(taskId, processInstanceId, DPUtil.stringify(audit(audit))); // 记录驳回原因
        runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId)
                .moveActivityIdsToSingleActivityId(activityIds, genesisActivityInstance.getActivityId()).changeState();
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).taskDefinitionKey(genesisActivityInstance.getActivityId()).active().singleResult();
        if (null != task) {
            taskService.setAssignee(task.getId(), submitter);
            taskService.setVariableLocal(task.getId(), "auditTaskId", taskId);
        }
        return ApiUtil.result(0, String.format("已驳回至[%s]节点", genesisActivityInstance.getActivityName()), workflowService.historicActivityInstance2node(genesisActivityInstance));
    }

    /**
     * 办结任务
     * modeComplete == true && modeReject == false => 办结
     * modeComplete == false && modeReject == false => 暂存
     * modeComplete == true && modeReject == true => 保存表单数据后驳回
     * modeComplete == false && modeReject == true => 不保存表单数据，直接驳回
     */
    public Map<String, Object> complete(String taskId, JsonNode form, JsonNode audit, JsonNode userInfo, boolean modeComplete, boolean modeReject) {
        if (DPUtil.empty(taskId)) return ApiUtil.result(1001, "参数异常", taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (null == task) {
            return ApiUtil.result(1002, "任务不存在", taskId);
        }
        if (null == task.getAssignee() || !task.getAssignee().equals(userInfo.get("uid").asText())) {
            return ApiUtil.result(1003, "当前任务不可办理", workflowService.task2node(task));
        }
        if (task.isSuspended()) return ApiUtil.result(1004, "任务已挂起", workflowService.task2node(task));
        Map<String, Object> variables = taskService.getVariables(task.getId());
        if (modeReject && !modeComplete) { // 直接驳回
            if (DPUtil.parseString(variables.get("submitter")).equals(userInfo.get("uid").asText())) {
                return ApiUtil.result(1005, "暂不支持驳回自己发起的流程", workflowService.task2node(task));
            }
            return reject(task.getProcessInstanceId(), task.getId(), audit, userInfo);
        }

        // 获取流程模型
        Workflow workflow = workflowService.info(DPUtil.parseInt(variables.get("workflowId")), false, true, true, true);
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1201, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1202, "当前流程关联表单异常", workflow);

        // 流程节点权限
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId()).singleResult();
        if (null == definition) return ApiUtil.result(1401, "当前流程定义异常", workflowService.task2node(task));
        FlowElement element = repositoryService.getBpmnModel(definition.getId()).getFlowElement(task.getTaskDefinitionKey());
        List<ExtensionElement> authorities = element.getExtensionElements().get("authority");
        if (null == authorities || 1 != authorities.size()) {
            return ApiUtil.result(1402, "节点权限配置异常", workflowService.processDefinition2node(definition));
        }
        JsonNode authority = DPUtil.parseJSON(authorities.get(0).getElementText());
        if (null == authority || !authority.isObject()) return ApiUtil.result(1403, "节点权限解析异常", authority);

        // 获取表单数据
        if (null == form || !form.isObject()) return ApiUtil.result(1601, "表单数据异常", form);
        ObjectNode origin = formService.info(frame, DPUtil.parseString(variables.get("formStorageId")));
        if (null == form || !form.isObject()) return ApiUtil.result(1602, "原表单数据异常", variables);
        Map<String, Object> result = formService.validate(frame.at("/widgets"), form, authority, origin, null);
        if (0 != (int) result.get("code")) return result;

        // 提交
        if (modeReject) {
            if (DPUtil.parseString(variables.get("submitter")).equals(userInfo.get("uid").asText())) {
                return ApiUtil.result(1005, "暂不支持驳回自己发起的流程", workflowService.task2node(task));
            }
        }
        origin = formService.save(frame, origin, userInfo.get("uid").asInt());
        if (modeReject) {
            return reject(task.getProcessInstanceId(), task.getId(), audit, userInfo);
        }
        identityService.setAuthenticatedUserId(userInfo.get("uid").asText());
        this.deleteComments(task.getId());
        taskService.addComment(task.getId(), task.getProcessInstanceId(), DPUtil.stringify(audit(audit)));
        if (modeComplete) {
            taskService.setVariable(task.getId(),"auditTaskId", task.getId()); // 全局变量，最后审批节点（非最新活动节点）
            ObjectNode taskVariables = formService.filtration(frame.at("/widgets"), origin, authority, "variable", false);
            taskService.complete(task.getId(), DPUtil.toJSON(taskVariables, Map.class), true);
            List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).active().list();
            for (Task activeTask : activeTasks) { // 记录任务来源
                taskService.setVariableLocal(activeTask.getId(), "auditTaskId", task.getId());
            }
        }
        return ApiUtil.result(0, null, workflowService.task2node(task));
    }

    public void deleteComments(String taskId) {
        List<Comment> taskComments = taskService.getTaskComments(taskId);
        for (Comment taskComment : taskComments) {
            taskService.deleteComment(taskComment.getId());
        }
    }

    /**
     * 历史任务，有我发起或经由我审批的全部任务
     */
    public Map<String, Object> searchHistory(Map<?, ?> param, Map<?, ?> config, JsonNode identity) {
        if (!identity.has("id")) return ApiUtil.result(1403, "获取当前用户信息失败", identity);
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .involvedUser(identity.get("id").asText()).notDeleted().orderByProcessInstanceStartTime().desc();
        String deploymentId = DPUtil.trim(DPUtil.parseString(param.get("deploymentId")));
        if(!DPUtil.empty(deploymentId)) {
            query.deploymentId(deploymentId);
        }
        String submitter = DPUtil.trim(DPUtil.parseString(param.get("submitter")));
        if(!DPUtil.empty(submitter)) {
            query.variableValueEquals("submitter", submitter);
        }
        String finishStatus = DPUtil.trim(DPUtil.parseString(param.get("finishStatus")));
        switch (finishStatus) {
            case "finished":
                query.finished();
                break;
            case "unfinished":
                query.unfinished();
                break;
        }
        long count = query.count();
        ArrayNode rows = workflowService.historicProcessInstance2node(count > 0 ? query.listPage((page - 1) * pageSize, pageSize) : new ArrayList<>());
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo("Id", "Info", rows, "startUserId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", count);
        result.put("rows", workflowService.fillDeployment(rows));
        return result;
    }

    public Map<String, Object> variables(List<HistoricVariableInstance> list) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (HistoricVariableInstance item : list) {
            variables.put(item.getVariableName(), item.getValue());
        }
        return variables;
    }

    public Map<String, Object> authority(FlowElement element, ObjectNode authority, JsonNode authorityDefaults) {
        if (null == element) return ApiUtil.result(1701, "节点获取异常", null);
        List<ExtensionElement> authorities = element.getExtensionElements().get("authority");
        if (null == authorities || 1 != authorities.size()) return ApiUtil.result(1702, "节点权限配置异常", element.getId());
        JsonNode authorityElement = DPUtil.parseJSON(authorities.get(0).getElementText());
        if (null == authorityElement || !authorityElement.isObject()) return ApiUtil.result(1703, "节点权限解析异常", authorityElement);
        Iterator<Map.Entry<String, JsonNode>> fields = authorityElement.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fields.next();
            String field = fieldEntry.getKey();
            ObjectNode node = authority.has(field) ? (ObjectNode) authority.get(field) : authority.putObject(field);
            Iterator<Map.Entry<String, JsonNode>> iterator = fieldEntry.getValue().fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                node.put(key, value.asBoolean(false) || (null == authorityDefaults ? false : authorityDefaults.at("/" + key).asBoolean(false)));
            }
        }
        return null;
    }

    public Map<String, Set<String>> taskAssignees(List<HistoricTaskInstance> historicTaskInstances) {
        Map<String, Set<String>> taskAssignees = new LinkedHashMap<>();
        for (HistoricTaskInstance instance : historicTaskInstances) {
            Set<String> assignees = taskAssignees.get(instance.getId());
            if (null == assignees) taskAssignees.put(instance.getId(), assignees = new HashSet<>());
            assignees.add(instance.getAssignee());
        }
        return taskAssignees;
    }

    public boolean isTaskAssignee(Map<String, Set<String>> taskAssignees, String taskId, String uid) {
        Set<String> assignees = taskAssignees.get(taskId);
        if (null == assignees) return false;
        return assignees.contains(uid);
    }

    /**
     * 流程详细信息
     * HistoricActivityInstance包含流程实例的所有活动，包括没有分配任务执行人的网管、开始事件和结束事件。
     * HistoricTaskInstance只包含该流程实例下跟人相关的活动，不包括未分配任务执行人的网关、开始事件和结束事件。
     */
    public Map<String, Object> process(String processInstanceId, String taskId, JsonNode userInfo, JsonNode authorityDefaults) {
        if (DPUtil.empty(processInstanceId)) return ApiUtil.result(1001, "参数异常", processInstanceId);
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId);
        String uid = userInfo.get("uid").asText();
        if (null == authorityDefaults) { // 无超级管理权限
            query.involvedUser(uid).notDeleted();
        }
        HistoricProcessInstance instance = query.singleResult();
        if (null == instance) return ApiUtil.result(1003, "流程实例不存在", processInstanceId);
        if (instance.getEndTime() != null && !DPUtil.empty(taskId)) {
            return ApiUtil.result(1004, "当前流程已结束", workflowService.historicProcessInstance2node(instance));
        }
        Map<String, Object> variables;
        if (DPUtil.empty(taskId)) {
            variables = variables(historyService.createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).list());
        } else {
            try {
                variables = taskService.getVariables(taskId);
            } catch (Exception e) {
                return ApiUtil.result(1005, "获取任务变量异常", e.getMessage());
            }
        }

        // 获取流程模型
        Workflow workflow = workflowService.info(DPUtil.parseInt(variables.get("workflowId")), false, true, true, true);
        if (null == workflow || workflow.getStatus() != 1) return ApiUtil.result(1201, "当前流程不可用", null);
        ObjectNode frame = workflow.getFormInfo();
        if (null == frame) return ApiUtil.result(1202, "当前流程关联表单异常", workflow);

        // 运行时流程实例
        ProcessInstance runtime = runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult();
        // 获取审批信息
        List<Comment> comments = taskService.getProcessInstanceComments(instance.getId());
        List<String> keepTaskIds = null; // 为null时拥有全部管理查看权限
        if (null == authorityDefaults ) {
            keepTaskIds = new ArrayList<>();
            if (!DPUtil.empty(taskId)) keepTaskIds.add(DPUtil.parseString(variables.get("auditTaskId")));
        }

        // 流程节点权限
        ObjectNode authority = DPUtil.objectNode();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().processDefinitionId(instance.getProcessDefinitionId()).singleResult();
        if (null == definition) return ApiUtil.result(1301, "当前流程定义异常", workflowService.historicProcessInstance2node(instance));
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(instance.getId()).list();
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(instance.getId()).list();
        Map<String, Set<String>> taskAssignees = taskAssignees(historicTaskInstances);
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            if (!DPUtil.empty(taskId) && taskId.equals(historicActivityInstance.getTaskId()) && !uid.equals(historicActivityInstance.getAssignee())) {
                return ApiUtil.result(1403, "无当前任务查看权限", workflowService.historicProcessInstance2node(instance));
            }
            if (null != keepTaskIds && uid.equals(historicActivityInstance.getAssignee())) keepTaskIds.add(historicActivityInstance.getTaskId());
            if ("userTask".equals(historicActivityInstance.getActivityType())) {
                if (null != authorityDefaults || isTaskAssignee(taskAssignees, historicActivityInstance.getTaskId(), uid)) { // 拥有超管权限或为当前登录用户
                    FlowElement element = repositoryService.getBpmnModel(definition.getId()).getFlowElement(historicActivityInstance.getActivityId());
                    Map<String, Object> result = authority(element, authority, authorityDefaults);
                    if (null != result) return result;
                }
            }
        }

        // 获取表单数据
        ObjectNode form = formService.info(frame, DPUtil.parseString(variables.get("formStorageId")));
        if (null == form || !form.isObject()) return ApiUtil.result(1601, "表单数据异常", variables);
        form = formService.filtration(frame.at("/widgets"), form, authority, "viewable", true);

        ObjectNode result = DPUtil.objectNode();
        result.put("id", workflow.getId());
        result.put("name", workflow.getName());
        result.put("content", workflow.getContent());
        result.put("description", workflow.getDescription());
        result.replace("form", form);
        result.put("formId", workflow.getFormId());
        result.replace("formInfo", workflow.getFormInfo());
        result.replace("authority", authority);
        result.replace("comments", audit(comments, keepTaskIds));
        result.replace("processInstanceInfo", workflowService.processInstance2node(runtime));
        result.replace("historicProcessInstanceInfo", workflowService.historicProcessInstance2node(instance));
        result.replace("historicTaskInstances", workflowService.historicTaskInstance2node(historicTaskInstances));
        result.replace("historicActivityInstances", workflowService.historicActivityInstance2node(historicActivityInstances));
        if (!fillUserInfo(result, "historicProcessInstanceInfo")) return ApiUtil.result(1601, "获取用户信息失败", result);
        return ApiUtil.result(0, null, result);
    }

}
