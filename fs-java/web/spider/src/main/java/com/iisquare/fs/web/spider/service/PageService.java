package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.PageDao;
import com.iisquare.fs.web.spider.entity.Page;
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
public class PageService extends JPAServiceBase {

    @Autowired
    private PageDao pageDao;
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

    public Page info(Integer id) {
        return info(pageDao, id);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(pageDao, json, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if(DPUtil.empty(code)) return ApiUtil.result(1001, "编码不能为空", code);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1002, "名称不能为空", name);
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        if(DPUtil.empty(url)) return ApiUtil.result(1003, "链接地址不能为空", url);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1004, "状态异常", status);
        Page info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Page();
        }
        Template template = templateService.info(DPUtil.parseInt(param.get("templateId")));
        if (null == template) return ApiUtil.result(1404, "所属模板不存在，请保存后继续", null);
        info.setTemplateId(template.getId());
        info.setCode(code);
        info.setName(name);
        info.setForceMode(DPUtil.parseBoolean(param.get("forceMode")) ? 1 : 0);
        info.setPriority(DPUtil.parseLong(param.get("priority")));
        info.setUrl(url);
        info.setCharset(DPUtil.parseString(param.get("charset")));
        info.setCollection(DPUtil.parseString(param.get("collection")));
        info.setBucket(DPUtil.parseString(param.get("bucket")));
        info.setConnectTimeout(DPUtil.parseInt(param.get("connectTimeout")));
        info.setSocketTimeout(DPUtil.parseInt(param.get("socketTimeout")));
        info.setIterateCount(DPUtil.parseInt(param.get("iterateCount")));
        info.setRetryCount(DPUtil.parseInt(param.get("retryCount")));
        info.setHeaders(DPUtil.parseString(param.get("headers")));
        info.setParser(DPUtil.parseString(param.get("parser")));
        info.setMapper(DPUtil.parseString(param.get("mapper")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(pageDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(pageDao, param, (Specification<Page>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("templateId"), DPUtil.parseInt(param.get("templateId"))));
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
            if (!DPUtil.empty(code)) {
                predicates.add(cb.like(root.get("code"),  code));
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
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.put("forceMode", DPUtil.parseBoolean(node.at("/forceMode").asInt()));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(pageDao, ids);
    }

}
