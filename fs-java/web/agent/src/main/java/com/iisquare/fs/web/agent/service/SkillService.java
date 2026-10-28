package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.agent.dao.SkillDao;
import com.iisquare.fs.web.agent.entity.Skill;
import com.iisquare.fs.web.agent.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SkillService extends JPAServiceBase {

    @Autowired
    SkillDao skillDao;
    @Autowired
    SkillVersionService versionService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Skill info(Integer id) {
        return info(skillDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Skill info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", null);
        JsonNode node = DPUtil.firstNode(format(DPUtil.toArrayNode(info)));
        return ApiUtil.result(0, null, node);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "技能名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Skill info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Skill();
        }
        info.setName(name);
        info.setLabels(DPUtil.implode(",", DPUtil.parseStringList(param.get("labels"))));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(skillDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(skillDao, param, (root, query, cb) -> {
            SpecificationHelper<Skill> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
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
            List<String> labels = DPUtil.parseStringList(node.at("/labels").asText(""));
            node.replace("labels", DPUtil.toJSON(labels));
            List<Integer> ids = DPUtil.parseIntList(node.at("/roleIds").asText(""));
            node.replace("roleIds", DPUtil.toJSON(ids));
        }
        return rows;
    }

    /**
     * 客户端可用技能：供桌面端 / 移动端等调用方使用。
     *
     * 只返回「已启用 + 授权角色命中」的技能基础信息，且刻意不下发 content —— 技能配置里
     * 可能有内部约定与凭据，调用方只需要名称、标签与描述来展示与选择。
     *
     * 同时给出 `versionCount` 与 `latestVersion`（版本数量与最新的启用版本）：
     * 客户端的版本下拉据此直接显示版本号，展开时再去取整份版本清单，不必为了显示一个版本号
     * 在进页面时把每个技能的版本都请求一遍。
     */
    public Map<String, Object> available(HttpServletRequest request) {
        List<Skill> rows = skillDao.findAll((root, query, cb) -> cb.equal(root.get("status"), 1),
                Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")));
        ArrayNode items = DPUtil.arrayNode();
        for (Skill info : rows) {
            if (!authorized(info, request)) continue;
            ObjectNode item = items.addObject();
            item.put("id", info.getId());
            item.put("name", DPUtil.parseString(info.getName()));
            item.set("labels", DPUtil.toJSON(DPUtil.parseStringList(info.getLabels())));
            item.put("description", DPUtil.parseString(info.getDescription()));
            item.put("updatedTime", null == info.getUpdatedTime() ? 0L : info.getUpdatedTime());
            item.put("versionCount", versionService.countEnabled(info.getId()));
            item.set("latestVersion", versionService.latestEnabled(info.getId()));
        }
        ObjectNode result = DPUtil.objectNode();
        result.set("rows", items);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 技能授权校验：没有配置授权角色时所有登录用户可用；配置后要求调用人的角色与之有交集。
     * 与编排应用的 authorized 同一套口径（mine 只在需要时取，避免无谓的 RPC）。
     */
    public boolean authorized(Skill info, HttpServletRequest request) {
        if (null == info) return false;
        Set<Integer> allowed = new LinkedHashSet<>(DPUtil.parseIntList(info.getRoleIds()));
        if (allowed.isEmpty()) return true;
        Set<Integer> mine = rbacService.roleIds(request);
        return !Collections.disjoint(allowed, mine);
    }

    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            for (Skill skill : skillDao.findAllById(ids)) {
                versionService.deleteBySkillId(skill.getId());
            }
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除技能版本失败", e.getMessage());
        }
        return remove(skillDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(skillDao, rows, properties);
    }

}
