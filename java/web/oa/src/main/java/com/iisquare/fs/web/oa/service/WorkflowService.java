package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.oa.dao.WorkflowDao;
import com.iisquare.fs.web.oa.entity.Workflow;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.runtime.ProcessInstance;
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
    public static final String BPMN_SUFFIX = ".bpmn20.xml";

    public String deploymentKey(Workflow info) {
        return "fs-" + info.getId();
    }

    public void deleteDeployment (String id, boolean cascade) {
        repositoryService.deleteDeployment(id, cascade);
    }

    public ObjectNode deployment2node(Deployment deployment) {
        if (null == deployment) return null;
        ObjectNode node = DPUtil.objectNode();
        node.put("parentDeploymentId", deployment.getParentDeploymentId());
        node.put("category", deployment.getCategory());
        node.put("derivedFrom", deployment.getDerivedFrom());
        node.put("derivedFromRoot", deployment.getDerivedFromRoot());
        node.put("engineVersion", deployment.getEngineVersion());
        node.put("id", deployment.getId());
        node.put("key", deployment.getKey());
        node.put("name", deployment.getName());
        node.put("tenantId", deployment.getTenantId());
        node.put("deploymentTime", deployment.getDeploymentTime().getTime());
        return node;
    }

    public ObjectNode processInstance2node(ProcessInstance instance) {
        if (null == instance) return null;
        ObjectNode node = DPUtil.objectNode();
        node.put("processDefinitionId", instance.getProcessDefinitionId());
        node.put("processDefinitionName", instance.getProcessDefinitionName());
        node.put("processDefinitionKey", instance.getProcessDefinitionKey());
        node.put("processDefinitionVersion", instance.getProcessDefinitionVersion());
        node.put("deploymentId", instance.getDeploymentId());
        node.put("businessKey", instance.getBusinessKey());
        node.put("isSuspended", instance.isSuspended());
        node.put("tenantId", instance.getTenantId());
        node.put("name", instance.getName());
        node.put("description", instance.getDescription());
        node.put("localizedName", instance.getLocalizedName());
        node.put("localizedDescription", instance.getLocalizedDescription());
        node.put("startTime", instance.getStartTime().getTime());
        node.put("startUserId", instance.getStartUserId());
        node.put("callbackId", instance.getCallbackId());
        node.put("callbackType", instance.getCallbackType());
        return node;
    }

    public ArrayNode deployment2node(List<Deployment> list) {
        ArrayNode result = DPUtil.arrayNode();
        for (Deployment deployment : list) {
            result.add(deployment2node(deployment));
        }
        return result;
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
        Deployment deployment;
        try {
            deployment = repositoryService.createDeployment().key(publishKey)
                    .name(info.getName()).addString(publishKey + BPMN_SUFFIX, info.getContent()).deploy();
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
            ServiceUtil.fillProperties(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
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
        Set<Integer> ids = ServiceUtil.getPropertyValues(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, Workflow> map = ServiceUtil.indexObjectList(workflowDao.findAllById(ids), Integer.class, Workflow.class, "id");
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
        Set<String> ids = ServiceUtil.getPropertyValues(list, String.class, properties);
        if(ids.size() < 1) return list;
        List<Deployment> deployments = repositoryService.createDeploymentQuery().deploymentIds(new ArrayList<>(ids)).list();
        Map<String, Deployment> map = ServiceUtil.indexObjectList(deployments, String.class, Deployment.class, "id");
        if(map.size() < 1) return list;
        for (Workflow item : list) {
            Deployment deployment = map.get(item.getDeploymentId());
            if(null == deployment) continue;
            item.setDeploymentInfo(deployment2node(deployment));
        }
        return list;
    }

}
