package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.SiteDao;
import com.iisquare.fs.web.spider.entity.Site;
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
public class SiteService extends JPAServiceBase {

    @Autowired
    private SiteDao siteDao;
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

    public Site info(Integer id) {
        return info(siteDao, id);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(siteDao, json, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称不能为空", name);
        String domain = DPUtil.trim(DPUtil.parseString(param.get("domain")));
        if(DPUtil.empty(domain)) return ApiUtil.result(1002, "域名不能为空", domain);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Site info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Site();
        }
        Template template = templateService.info(DPUtil.parseInt(param.get("templateId")));
        if (null == template) return ApiUtil.result(1404, "所属模板不存在，请保存后继续", null);
        info.setTemplateId(template.getId());
        info.setName(name);
        info.setDomain(domain);
        info.setCharset(DPUtil.parseString(param.get("charset")));
        info.setCollection(DPUtil.parseString(param.get("collection")));
        info.setBucket(DPUtil.parseString(param.get("bucket")));
        info.setRetainQuery(DPUtil.parseBoolean(param.get("retainQuery")) ? 1 : 0);
        info.setRetainAnchor(DPUtil.parseBoolean(param.get("retainAnchor")) ? 1 : 0);
        info.setConnectTimeout(DPUtil.parseInt(param.get("connectTimeout")));
        info.setSocketTimeout(DPUtil.parseInt(param.get("socketTimeout")));
        info.setIterateCount(DPUtil.parseInt(param.get("iterateCount")));
        info.setRetryCount(DPUtil.parseInt(param.get("retryCount")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(siteDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(siteDao, param, (Specification<Site>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("templateId"), DPUtil.parseInt(param.get("templateId"))));
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"),  name));
            }
            String domain = DPUtil.trim(DPUtil.parseString(param.get("domain")));
            if (!DPUtil.empty(domain)) {
                predicates.add(cb.like(root.get("domain"),  domain));
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
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.put("retainQuery", DPUtil.parseBoolean(node.at("/retainQuery").asInt()));
            node.put("retainAnchor", DPUtil.parseBoolean(node.at("/retainAnchor").asInt()));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(siteDao, ids);
    }

}
