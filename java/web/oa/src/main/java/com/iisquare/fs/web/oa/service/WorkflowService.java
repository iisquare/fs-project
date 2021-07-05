package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.oa.dao.WorkflowDao;
import com.iisquare.fs.web.oa.entity.Workflow;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Service
public class WorkflowService extends ServiceBase {

    @Autowired
    private WorkflowDao workflowDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private FormService formService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ManagementService managementService; // 引擎管理服务，和具体业务无关，管理引擎。

    public static final String BPMN_SUFFIX = ".bpmn20.xml";

    public ObjectNode state() {
        ObjectNode state = DPUtil.objectNode();
        state.replace("tableCount", DPUtil.convertJSON(managementService.getTableCount()));
        return state;
    }

    public String deploymentKey(Workflow info) {
        return "fs-" + info.getId();
    }

    public void deleteDeployment (String id, boolean cascade) {
        repositoryService.deleteDeployment(id, cascade);
    }

    public ArrayNode comment2node(List<Comment> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (Comment comment : list) {
            result.add(comment2node(comment));
        }
        return result;
    }

    public ObjectNode comment2node(Comment comment) {
        ObjectNode node = DPUtil.objectNode();
        if (null == comment) return node;
        node.put("fullMessage", comment.getFullMessage());
        node.put("id", comment.getId());
        node.put("processInstanceId", comment.getProcessInstanceId());
        node.put("taskId", comment.getTaskId());
        node.put("time", comment.getTime().getTime());
        node.put("type", comment.getType());
        node.put("userId", comment.getUserId());
        return node;
    }

    public ArrayNode deployment2node(List<Deployment> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (Deployment deployment : list) {
            result.add(deployment2node(deployment));
        }
        return result;
    }

    public ObjectNode deployment2node(Deployment deployment) {
        ObjectNode node = DPUtil.objectNode();
        if (null == deployment) return node;
        node.put("category", deployment.getCategory());
        node.put("deploymentTime", deployment.getDeploymentTime().getTime());
        node.put("derivedFrom", deployment.getDerivedFrom());
        node.put("derivedFromRoot", deployment.getDerivedFromRoot());
        node.put("engineVersion", deployment.getEngineVersion());
        node.put("id", deployment.getId());
        node.put("key", deployment.getKey());
        node.put("name", deployment.getName());
        node.put("parentDeploymentId", deployment.getParentDeploymentId());
        node.put("tenantId", deployment.getTenantId());
        return node;
    }

    public ArrayNode processDefinition2node(List<ProcessDefinition> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (ProcessDefinition definition : list) {
            result.add(processDefinition2node(definition));
        }
        return result;
    }

    public ObjectNode processDefinition2node(ProcessDefinition definition) {
        ObjectNode node = DPUtil.objectNode();
        if (null == definition) return node;
        node.put("category", definition.getCategory());
        node.put("deploymentId", definition.getDeploymentId());
        node.put("derivedFrom", definition.getDerivedFrom());
        node.put("derivedFromRoot", definition.getDerivedFromRoot());
        node.put("derivedVersion", definition.getDerivedVersion());
        node.put("description", definition.getDescription());
        node.put("diagramResourceName", definition.getDiagramResourceName());
        node.put("engineVersion", definition.getEngineVersion());
        node.put("id", definition.getId());
        node.put("key", definition.getKey());
        node.put("name", definition.getName());
        node.put("resourceName", definition.getResourceName());
        node.put("tenantId", definition.getTenantId());
        node.put("version", definition.getVersion());
        return node;
    }

    public ArrayNode processInstance2node(List<ProcessInstance> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (ProcessInstance instance : list) {
            result.add(processInstance2node(instance));
        }
        return result;
    }

    public ObjectNode processInstance2node(ProcessInstance instance) {
        ObjectNode node = DPUtil.objectNode();
        if (null == instance) return node;
        node.put("activityId", instance.getActivityId());
        node.put("businessKey", instance.getBusinessKey());
        node.put("callbackId", instance.getCallbackId());
        node.put("callbackType", instance.getCallbackType());
        node.put("deploymentId", instance.getDeploymentId());
        node.put("description", instance.getDescription());
        node.put("id", instance.getId());
        node.put("isSuspended", instance.isSuspended());
        node.put("localizedDescription", instance.getLocalizedDescription());
        node.put("localizedName", instance.getLocalizedName());
        node.put("name", instance.getName());
        node.put("parentId", instance.getParentId());
        node.put("processDefinitionId", instance.getProcessDefinitionId());
        node.put("processDefinitionKey", instance.getProcessDefinitionKey());
        node.put("processDefinitionName", instance.getProcessDefinitionName());
        node.put("processDefinitionVersion", instance.getProcessDefinitionVersion());
        node.put("processInstanceId", instance.getProcessInstanceId());
        node.put("propagatedStageInstanceId", instance.getPropagatedStageInstanceId());
        node.put("referenceId", instance.getReferenceId());
        node.put("referenceType", instance.getReferenceType());
        node.put("rootProcessInstanceId", instance.getRootProcessInstanceId());
        node.put("startTime", instance.getStartTime().getTime());
        node.put("startUserId", instance.getStartUserId());
        node.put("superExecutionId", instance.getSuperExecutionId());
        node.put("tenantId", instance.getTenantId());
        return node;
    }

    public ArrayNode historicProcessInstance2node(List<HistoricProcessInstance> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (HistoricProcessInstance instance : list) {
            result.add(historicProcessInstance2node(instance));
        }
        return result;
    }

    public ObjectNode historicProcessInstance2node(HistoricProcessInstance instance) {
        ObjectNode node = DPUtil.objectNode();
        if (null == instance) return node;
        node.put("callbackId", instance.getCallbackId());
        node.put("callbackType", instance.getCallbackType());
        node.put("deleteReason", instance.getDeleteReason());
        node.put("deploymentId", instance.getDeploymentId());
        node.put("description", instance.getDescription());
        node.put("durationInMillis", instance.getDurationInMillis());
        node.put("endActivityId", instance.getEndActivityId());
        node.put("endTime", null == instance.getEndTime() ? null : instance.getEndTime().getTime());
        node.put("id", instance.getId());
        node.put("name", instance.getName());
        node.put("processDefinitionId", instance.getProcessDefinitionId());
        node.put("processDefinitionKey", instance.getProcessDefinitionKey());
        node.put("processDefinitionName", instance.getProcessDefinitionName());
        node.put("processDefinitionVersion", instance.getProcessDefinitionVersion());
        node.put("referenceId", instance.getReferenceId());
        node.put("referenceType", instance.getReferenceType());
        node.put("startActivityId", instance.getStartActivityId());
        node.put("startTime", null == instance.getStartTime() ? null : instance.getStartTime().getTime());
        node.put("startUserId", instance.getStartUserId());
        node.put("superProcessInstanceId", instance.getSuperProcessInstanceId());
        node.put("tenantId", instance.getTenantId());
        node.put("businessKey", instance.getBusinessKey());
        return node;
    }

    public ArrayNode historicActivityInstance2node(List<HistoricActivityInstance> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (HistoricActivityInstance instance : list) {
            result.add(historicActivityInstance2node(instance));
        }
        return result;
    }

    public ObjectNode historicActivityInstance2node(HistoricActivityInstance instance) {
        ObjectNode node = DPUtil.objectNode();
        if (null == instance) return node;
        node.put("activityId", instance.getActivityId());
        node.put("activityName", instance.getActivityName());
        node.put("activityType", instance.getActivityType());
        node.put("assignee", instance.getAssignee());
        node.put("calledProcessInstanceId", instance.getCalledProcessInstanceId());
        node.put("deleteReason", instance.getDeleteReason());
        node.put("durationInMillis", instance.getDurationInMillis());
        node.put("endTime", null == instance.getEndTime() ? null : instance.getEndTime().getTime());
        node.put("executionId", instance.getExecutionId());
        node.put("id", instance.getId());
        node.put("processDefinitionId", instance.getProcessDefinitionId());
        node.put("processInstanceId", instance.getProcessInstanceId());
        node.put("startTime", null == instance.getStartTime() ? null : instance.getStartTime().getTime());
        node.put("taskId", instance.getTaskId());
        node.put("tenantId", instance.getTenantId());
        node.put("time", null == instance.getTime() ? null : instance.getTime().getTime());
        node.put("transactionOrder", instance.getTransactionOrder());
        return node;
    }

    public ArrayNode historicTaskInstance2node(List<HistoricTaskInstance> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (HistoricTaskInstance instance : list) {
            result.add(historicTaskInstance2node(instance));
        }
        return result;
    }

    public ObjectNode historicTaskInstance2node(HistoricTaskInstance instance) {
        ObjectNode node = DPUtil.objectNode();
        if (null == instance) return node;
        node.put("assignee", instance.getAssignee());
        node.put("category", instance.getCategory());
        node.put("claimTime", instance.getClaimTime() == null ? null : instance.getClaimTime().getTime());
        node.put("createTime", instance.getCreateTime().getTime());
        node.put("deleteReason", instance.getDeleteReason());
        node.put("description", instance.getDescription());
        node.put("dueDate", instance.getDueDate() == null ? null : instance.getDueDate().getTime());
        node.put("durationInMillis", instance.getDurationInMillis());
        node.put("endTime", null == instance.getEndTime() ? null : instance.getEndTime().getTime());
        node.put("executionId", instance.getExecutionId());
        node.put("formKey", instance.getFormKey());
        node.put("id", instance.getId());
        node.put("name", instance.getName());
        node.put("owner", instance.getOwner());
        node.put("parentTaskId", instance.getParentTaskId());
        node.put("priority", instance.getPriority());
        node.put("processDefinitionId", instance.getProcessDefinitionId());
        node.put("processInstanceId", instance.getProcessInstanceId());
        node.put("propagatedStageInstanceId", instance.getPropagatedStageInstanceId());
        node.put("scopeDefinitionId", instance.getScopeDefinitionId());
        node.put("scopeId", instance.getScopeId());
        node.put("scopeType", instance.getScopeType());
        node.put("subScopeId", instance.getSubScopeId());
        node.put("taskDefinitionId", instance.getTaskDefinitionId());
        node.put("taskDefinitionKey", instance.getTaskDefinitionKey());
        node.put("tenantId", instance.getTenantId());
        node.put("time", instance.getTime() == null ? null : instance.getTime().getTime());
        node.put("workTimeInMillis", instance.getWorkTimeInMillis());
        return node;
    }

    public ArrayNode task2node(List<Task> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (Task task : list) {
            result.add(task2node(task));
        }
        return result;
    }

    public ObjectNode task2node(Task task) {
        ObjectNode node = DPUtil.objectNode();
        if (null == task) return node;
        node.put("assignee", task.getAssignee());
        node.put("category", task.getCategory());
        node.put("claimTime", null == task.getClaimTime() ? null : task.getClaimTime().getTime());
        node.put("createTime", null == task.getCreateTime() ? null : task.getCreateTime().getTime());
        node.put("delegationState", null == task.getDelegationState() ? null : task.getDelegationState().name());
        node.put("description", task.getDescription());
        node.put("dueDate", null == task.getDueDate() ? null : task.getDueDate().getTime());
        node.put("executionId", task.getExecutionId());
        node.put("formKey", task.getFormKey());
        node.put("id", task.getId());
        node.put("name", task.getName());
        node.put("owner", task.getOwner());
        node.put("parentTaskId", task.getParentTaskId());
        node.put("priority", task.getPriority());
        node.put("processDefinitionId", task.getProcessDefinitionId());
        node.put("processInstanceId", task.getProcessInstanceId());
        node.put("propagatedStageInstanceId", task.getPropagatedStageInstanceId());
        node.put("scopeDefinitionId", task.getScopeDefinitionId());
        node.put("scopeId", task.getScopeId());
        node.put("scopeType", task.getScopeType());
        node.put("subScopeId", task.getSubScopeId());
        node.put("taskDefinitionId", task.getTaskDefinitionId());
        node.put("taskDefinitionKey", task.getTaskDefinitionKey());
        node.put("tenantId", task.getTenantId());
        return node;
    }

    public ArrayNode fillDeployment(ArrayNode array) {
        if (null == array || array.size() < 1) return array;
        Set<String> processDefinitionIds = DPUtil.values(array, String.class, "processDefinitionId");
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().processDefinitionIds(processDefinitionIds).list();
        Map<String, String> processDefinitionId2deploymentId = new LinkedHashMap<>();
        for (ProcessDefinition definition : definitions) {
            processDefinitionId2deploymentId.put(definition.getId(), definition.getDeploymentId());
        }
        Map<String, Deployment> deployments = DPUtil.list2map(
                repositoryService.createDeploymentQuery().deploymentIds(new ArrayList<>(processDefinitionId2deploymentId.values())).list(),
                String.class, "id");
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            node.replace("deploymentInfo", deployment2node(
                    deployments.get(processDefinitionId2deploymentId.get(node.get("processDefinitionId").asText()))));
        }
        return array;
    }

    public Map<String, Object> searchHistory(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc();
        String deploymentId = DPUtil.trim(DPUtil.parseString(param.get("deploymentId")));
        if(!DPUtil.empty(deploymentId)) {
            query.deploymentId(deploymentId);
        }
        String submitter = DPUtil.trim(DPUtil.parseString(param.get("submitter")));
        if(!DPUtil.empty(submitter)) {
            query.variableValueEquals("submitter", submitter);
        }
        String involvedUserId = DPUtil.trim(DPUtil.parseString(param.get("involvedUserId")));
        if(!DPUtil.empty(involvedUserId)) {
            query.involvedUser(involvedUserId);
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
        String deleteStatus = DPUtil.trim(DPUtil.parseString(param.get("deleteStatus")));
        switch (deleteStatus) {
            case "deleted":
                query.deleted();
                break;
            case "notDeleted":
                query.notDeleted();
                break;
        }
        long count = query.count();
        ArrayNode rows = historicProcessInstance2node(count > 0 ? query.listPage((page - 1) * pageSize, pageSize) : new ArrayList<>());
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "startUserId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", count);
        result.put("rows", fillProcessInstanceWithHistory(fillDeployment(rows)));
        return result;
    }

    public ArrayNode fillProcessInstanceWithHistory(ArrayNode array) {
        Set<String> ids = DPUtil.values(array, String.class, "id");
        if (ids.size() < 1) return array;
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processInstanceIds(ids);
        ObjectNode instances = DPUtil.array2object(processInstance2node(query.list()), "id");
        return DPUtil.fillValues(array, true, new String[]{"id"}, new String[]{"processInstanceInfo"}, instances);
    }

    public Map<?, ?> searchDeployment(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        DeploymentQuery query = repositoryService.createDeploymentQuery();
        String key = DPUtil.parseString(param.get("key"));
        if (!DPUtil.empty(key)) query.deploymentKey(key);
        String name = DPUtil.parseString(param.get("name"));
        if (!DPUtil.empty(name)) query.deploymentNameLike(name);
        long count = query.count();
        List<Deployment> rows = count > 0 ? query.listPage((page - 1) * pageSize, pageSize) : new ArrayList<>();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", count);
        result.put("rows", deployment2node(rows));
        return result;
    }

    public Map<String, Object> deployment(Integer id, int uid) {
        Workflow info = info(id, false, false, false, false);
        if (null == info) return ApiUtil.result(1404, "流程信息不存在", id);
        String publishKey = deploymentKey(info);
        String xml = info.getContent();
        xml = xml.replaceAll("&lt;!\\[CDATA\\[", "<![CDATA[");
        xml = xml.replaceAll("\\]\\]&gt;", "]]>");
        Deployment deployment;
        try {
            deployment = repositoryService.createDeployment().key(publishKey)
                    .name(info.getName()).addString(publishKey + BPMN_SUFFIX, xml).deploy();
        } catch (Exception e) {
            return ApiUtil.result(1501, "流程部署异常", e.getMessage());
        }
        if (null == deployment) return ApiUtil.result(1001, "流程部署失败", info);
        info.setDeploymentId(deployment.getId());
        info.setDeploymentUid(uid);
        info = workflowDao.save(info);
        if (null == info) return ApiUtil.result(1500, "流程信息记录失败", id);
        return ApiUtil.result(0, null, deployment);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Workflow> data = workflowDao.findAll((Specification<Workflow>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if(id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), -1));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            int formId = DPUtil.parseInt(param.get("formId"));
            if(!"".equals(DPUtil.parseString(param.get("formId")))) {
                predicates.add(cb.equal(root.get("formId"), formId));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<Workflow> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid", "deploymentUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(config.get("withDeploymentInfo"))) fillDeployment(rows);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", DPUtil.empty(config.get("withBrief")) ? rows : brief(rows));
        return result;
    }

    public ArrayNode brief(List<Workflow> rows) {
        ArrayNode nodes = DPUtil.arrayNode();
        for (Workflow row : rows) {
            ObjectNode node = nodes.addObject();
            node.put("id", row.getId());
            node.put("name", row.getName());
            node.put("deploymentInfo", row.getDeploymentInfo());
            node.put("description", row.getDescription());
        }
        return nodes;
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public Map<?, ?> finishStatus(String level) {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("finished", "已结束");
        status.put("unfinished", "未结束");
        return status;
    }

    public Map<?, ?> deleteStatus(String level) {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("deleted", "已删除");
        status.put("notDeleted", "未删除");
        return status;
    }

    public Workflow info(Integer id, boolean withDeployment, boolean withForm, boolean withFormDetail, boolean withFormRemote) {
        if(null == id || id < 1) return null;
        Workflow info = JPAUtil.findById(workflowDao, id, Workflow.class);
        if (null == info) return null;
        if (withDeployment && !DPUtil.empty(info.getDeploymentId())) {
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(info.getDeploymentId()).singleResult();
            info.setDeploymentInfo(deployment2node(deployment));
        }
        if (withForm) {
            info.setFormInfo(formService.frame(info.getFormId(), DPUtil.objectNode(), withFormDetail, withFormRemote));
        }
        return info;
    }

    public Workflow save(Workflow info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
            info.setDeploymentId("");
            info.setDeploymentUid(0);
        }
        return workflowDao.save(info);
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Workflow> list = workflowDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Workflow item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        workflowDao.saveAll(list);
        return true;
    }

    public List<?> fillInfo(List<?> list, String ...properties) {
        if(null == list || list.size() < 1 || properties.length < 1) return list;
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, Workflow> map = DPUtil.list2map(workflowDao.findAllById(ids), Integer.class, Workflow.class, "id");
        if(map.size() < 1) return list;
        for (Object item : list) {
            for (String property : properties) {
                Workflow info = map.get(ReflectUtil.getPropertyValue(item, property));
                if(null == info) continue;
                ReflectUtil.setPropertyValue(item, property + "Name", null, new Object[]{info.getName()});
            }
        }
        return list;
    }

    public List<Workflow> fillDeployment(List<Workflow> list) {
        if(null == list || list.size() < 1) return list;
        String[] properties = new String[]{"deploymentId"};
        Set<String> ids = DPUtil.values(list, String.class, properties);
        if(ids.size() < 1) return list;
        List<Deployment> deployments = repositoryService.createDeploymentQuery().deploymentIds(new ArrayList<>(ids)).list();
        Map<String, Deployment> map = DPUtil.list2map(deployments, String.class, Deployment.class, "id");
        if(map.size() < 1) return list;
        for (Workflow item : list) {
            Deployment deployment = map.get(item.getDeploymentId());
            if(null == deployment) continue;
            item.setDeploymentInfo(deployment2node(deployment));
        }
        return list;
    }

    public Map<String, Object> toggleProcessInstance(String processInstanceId, boolean active) {
        if (DPUtil.empty(processInstanceId)) return ApiUtil.result(1001, "参数异常", processInstanceId);
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (null == instance || instance.isEnded()) return ApiUtil.result(1002, "流程实例不可用", processInstanceId);
        if (active) {
            if (!instance.isSuspended()) return ApiUtil.result(1101, "流程实例已激活", processInstance2node(instance));
            runtimeService.activateProcessInstanceById(processInstanceId);
        } else {
            if (instance.isSuspended()) return ApiUtil.result(1102, "流程实例已暂停", processInstance2node(instance));
            runtimeService.suspendProcessInstanceById(processInstanceId);
        }
        return ApiUtil.result(0, null, processInstance2node(instance));
    }

    /**
     * 撤销流程实例（通过deleteReason is null判断流程是否被删除）
     */
    public Map<String, Object> deleteProcessInstance(String processInstanceId, String deleteReason) {
        if (DPUtil.empty(processInstanceId)) return ApiUtil.result(1001, "参数异常", processInstanceId);
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (null == instance || instance.isEnded()) return ApiUtil.result(1002, "流程实例不可用", processInstanceId);
        if (DPUtil.empty(deleteReason)) return ApiUtil.result(1003, "请填写撤销原因", processInstanceId);
        runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
        return ApiUtil.result(0, "撤销成功", processInstance2node(instance));
    }

    /**
     * 删除流程实例
     */
    public Map<String, Object> deleteHistoricProcessInstance(String processInstanceId) {
        if (DPUtil.empty(processInstanceId)) return ApiUtil.result(1001, "参数异常", processInstanceId);
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (null == instance) return ApiUtil.result(1002, "流程实例不可用", processInstanceId);
        if (null == instance.getEndTime()) return ApiUtil.result(1003, "流程运行中，暂不可删除", historicProcessInstance2node(instance));
        historyService.deleteHistoricProcessInstance(instance.getId());
        return ApiUtil.result(0, String.format("流程[%s]删除成功", instance.getId()), historicProcessInstance2node(instance));
    }

}
