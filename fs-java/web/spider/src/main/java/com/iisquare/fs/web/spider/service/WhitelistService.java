package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.WhitelistDao;
import com.iisquare.fs.web.spider.entity.Template;
import com.iisquare.fs.web.spider.entity.Whitelist;
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
public class WhitelistService extends JPAServiceBase {

    @Autowired
    private WhitelistDao whitelistDao;
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

    public Whitelist info(Integer id) {
        return info(whitelistDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        String domain = DPUtil.trim(DPUtil.parseString(param.get("domain")));
        if(DPUtil.empty(domain)) domain = "*";
        String path = DPUtil.trim(DPUtil.parseString(param.get("path")));
        if(DPUtil.empty(path)) path = "*";
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Whitelist info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Whitelist();
        }
        Template template = templateService.info(DPUtil.parseInt(param.get("templateId")));
        if (null == template) return ApiUtil.result(1404, "所属模板不存在，请保存后继续", null);
        info.setTemplateId(template.getId());
        info.setName(name);
        info.setDomain(domain);
        info.setPath(path);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(whitelistDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(whitelistDao, param, (Specification<Whitelist>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("templateId"), DPUtil.parseInt(param.get("templateId"))));
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String domain = DPUtil.trim(DPUtil.parseString(param.get("domain")));
            if (!DPUtil.empty(domain)) {
                predicates.add(cb.like(root.get("domain"),  domain));
            }
            String path = DPUtil.trim(DPUtil.parseString(param.get("path")));
            if (!DPUtil.empty(path)) {
                predicates.add(cb.like(root.get("path"),  path));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public boolean remove(List<Integer> ids) {
        return remove(whitelistDao, ids);
    }

}
