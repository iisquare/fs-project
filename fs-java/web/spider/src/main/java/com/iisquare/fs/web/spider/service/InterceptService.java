package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.InterceptDao;
import com.iisquare.fs.web.spider.entity.Intercept;
import com.iisquare.fs.web.spider.entity.Template;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InterceptService extends JPAServiceBase {

    @Autowired
    private InterceptDao interceptDao;
    @Autowired
    TemplateService templateService;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public Intercept info(Integer id) {
        return info(interceptDao, id);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(interceptDao, json, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称不能为空", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Intercept info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Intercept();
        }
        Template template = templateService.info(DPUtil.parseInt(param.get("templateId")));
        if (null == template) return ApiUtil.result(1404, "所属模板不存在，请保存后继续", null);
        info.setTemplateId(template.getId());
        info.setCode(DPUtil.parseInt(param.get("code")));
        info.setName(name);
        info.setCharset(DPUtil.parseString(param.get("charset")));
        info.setCollection(DPUtil.parseString(param.get("collection")));
        info.setParser(DPUtil.parseString(param.get("parser")));
        info.setAssistant(DPUtil.parseString(param.get("assistant")));
        info.setMapper(DPUtil.parseString(param.get("mapper")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(interceptDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(interceptDao, param, (Specification<Intercept>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("templateId"), DPUtil.parseInt(param.get("templateId"))));
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            int code = DPUtil.parseInt(param.get("code"));
            if (!"".equals(DPUtil.parseString(param.get("code")))) {
                predicates.add(cb.equal(root.get("code"), code));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(interceptDao, ids);
    }

}
