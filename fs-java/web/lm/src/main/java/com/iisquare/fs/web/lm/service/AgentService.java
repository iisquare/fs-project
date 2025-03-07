package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.AgentDao;
import com.iisquare.fs.web.lm.entity.Agent;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class AgentService extends JPAServiceBase {

    @Autowired
    private AgentDao agentDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    public ObjectNode listByIdentity(HttpServletRequest request) {
        ObjectNode result = DPUtil.objectNode();
        JsonNode identity = rbacService.identity();
        if (!identity.has("id")) return result;
        Set<Integer> roleIds = DPUtil.values(identity.at("/roles"), Integer.class, "id");
        if (roleIds.size() == 0) return result;
        List<Agent> agents = agentDao.findAll((Specification<Agent>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            List<Predicate> ors = new ArrayList<>();
            for (Integer roleId : roleIds) {
                ors.add(cb.gt(cb.function("FIND_IN_SET", Integer.class, cb.literal(roleId), root.get("roleIds")), 0));
            }
            if (ors.size() > 0) {
                predicates.add(cb.or(predicates.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("name")));
        boolean permit = rbacService.hasPermit(request, "lm", "chat", "demo");
        for (Agent agent : agents) {
            ObjectNode item = result.putObject(String.valueOf(agent.getId()));
            item.put("id", agent.getId());
            item.put("name", agent.getName());
            item.put("description", agent.getDescription());
            if (!permit) continue;
            item.put("systemPrompt", agent.getSystemPrompt());
            item.put("maxTokens", agent.getMaxTokens());
            item.put("temperature", agent.getTemperature());
        }
        return result;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Agent info(Integer id) {
        return info(agentDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "智能体名称异常", name);
        String model = DPUtil.trim(DPUtil.parseString(param.get("model")));
        if(DPUtil.empty(model)) return ApiUtil.result(1002, "模型名称不能为空", model);
        String token = DPUtil.trim(DPUtil.parseString(param.get("token")));
        if(DPUtil.empty(token)) return ApiUtil.result(1003, "认证标识不能为空", token);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Agent info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Agent();
        }
        info.setName(name);
        info.setModel(model);
        info.setToken(token);
        info.setSystemPrompt(DPUtil.parseString(param.get("systemPrompt")));
        info.setMaxTokens(DPUtil.parseInt(param.get("maxTokens")));
        info.setTemperature(DPUtil.parseFloat(param.get("temperature")));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(agentDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(agentDao, param, (root, query, cb) -> {
            SpecificationHelper<Agent> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("model").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withRoles"))) {
            rbacService.fillInfos(rows);
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<Integer> ids = DPUtil.parseIntList(node.at("/roleIds").asText(""));
            node.replace("roleIds", DPUtil.toJSON(ids));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(agentDao, ids);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(agentDao, json, properties);
    }

}
