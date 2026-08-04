package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.SkillDao;
import com.iisquare.fs.web.lm.entity.Skill;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("default", "默认");
        return types;
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
        String type = DPUtil.parseString(param.get("type"));
        if (!types().containsKey(type)) return ApiUtil.result(1002, "技能类型异常", type);
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
        info.setType(type);
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
            helper.equalWithIntNotEmpty("status").like("name").equal("type");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
            DPUtil.fillValues(rows, "type", "typeText", types());
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

    public boolean remove(List<Integer> ids) {
        for (Skill skill : skillDao.findAllById(ids)) {
            versionService.deleteBySkillId(skill.getId());
        }
        return remove(skillDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(skillDao, rows, properties);
    }

}
