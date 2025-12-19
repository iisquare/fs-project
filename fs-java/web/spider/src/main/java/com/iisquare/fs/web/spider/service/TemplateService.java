package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.*;
import com.iisquare.fs.web.spider.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class TemplateService extends JPAServiceBase {

    @Autowired
    private TemplateDao templateDao;
    @Autowired
    RateDao rateDao;
    @Autowired
    SiteDao siteDao;
    @Autowired
    WhitelistDao whitelistDao;
    @Autowired
    BlacklistDao blacklistDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    NodeService nodeService;
    @Autowired
    RateService rateService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("pan", "泛采集");
        types.put("target", "定向采集");
        return types;
    }

    public Template info(Integer id) {
        return info(templateDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if (!types().containsKey(type)) return ApiUtil.result(1002, "类型异常", type);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Template info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Template();
            info.setType(type);
            info.setContent("");
        }
        info.setName(name);
        info.setRateId(DPUtil.parseInt(param.get("rateId")));
        info.setMaxThreads(DPUtil.parseInt(param.get("maxThreads")));
        info.setPriority(DPUtil.parseLong(param.get("priority")));
        info.setMinHalt(DPUtil.parseInt(param.get("minHalt")));
        info.setMaxHalt(DPUtil.parseInt(param.get("maxHalt")));
        info.setParams(DPUtil.stringify(param.get("params")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(templateDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(templateDao, param, (Specification<Template>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if (!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"),  type));
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
        if(!DPUtil.empty(args.get("withTypeText"))) {
            DPUtil.fillValues(rows, "type", "typeText", types());
        }
        if(!DPUtil.empty(args.get("withRateInfo"))) {
            rateService.fillInfo(rows, "rateId");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("params", DPUtil.parseJSON(node.at("/params").asText(), k -> DPUtil.objectNode()));
        }
        return rows;
    }

    public boolean delete(List<Integer> ids) {
        removeByParentId(rateDao, "templateId", ids);
        removeByParentId(siteDao, "templateId", ids);
        removeByParentId(whitelistDao, "templateId", ids);
        removeByParentId(blacklistDao, "templateId", ids);
        return remove(templateDao, ids);
    }

    public Map<String, Object> publish(Map<?, ?> param, HttpServletRequest request) {
        Template template = info(DPUtil.parseInt(param.get("id")));
        if (null == template || 1 != template.getStatus()) {
            return ApiUtil.result(1401, "模板不存在或已禁用", null);
        }
        ObjectNode node = DPUtil.objectNode();
        node.put("id", template.getId());
        node.put("name", template.getName());
        node.put("type", template.getType());
        node.put("rateId", template.getRateId());
        node.put("maxThreads", template.getMaxThreads());
        node.put("priority", template.getPriority());
        node.put("minHalt", template.getMinHalt());
        node.put("maxHalt", template.getMaxHalt());
        node.replace("params", DPUtil.parseJSON(template.getParams(), j -> DPUtil.objectNode()));
        int templateId = template.getId();
        if (template.getType().equals("pan")) {
            ObjectNode sites = node.putObject("sites");
            List<Site> siteList = siteDao.findAll((Specification<Site>) (root, query, cb) -> cb.and(
                    cb.equal(root.get("templateId"), templateId),
                    cb.equal(root.get("status"), 1)
            ));
            for (Site site : siteList) {
                ObjectNode item = DPUtil.objectNode();
                item.put("id", site.getId());
                item.put("domain", site.getDomain());
                item.put("charset", site.getCharset());
                item.put("collection", site.getCollection());
                item.put("bucket", site.getBucket());
                sites.replace(site.getDomain(), item);
            }
            ObjectNode whitelists = node.putObject("whitelists");
            List<Whitelist> whitelistList = whitelistDao.findAll((Specification<Whitelist>) (root, query, cb) -> cb.and(
                    cb.equal(root.get("templateId"), templateId),
                    cb.equal(root.get("status"), 1)
            ));
            for (Whitelist whitelist : whitelistList) {
                ObjectNode item = DPUtil.objectNode();
                item.put("id", whitelist.getId());
                item.put("domain", whitelist.getDomain());
                item.put("regexDomain", regexDomain(whitelist.getDomain()));
                item.put("path", whitelist.getPath());
                item.put("regexPath", regexPath(whitelist.getPath()));
                whitelists.replace(String.valueOf(whitelist.getId()), item);
            }
            ObjectNode blacklists = node.putObject("blacklists");
            List<Blacklist> blacklistList = blacklistDao.findAll((Specification<Blacklist>) (root, query, cb) -> cb.and(
                    cb.equal(root.get("templateId"), templateId),
                    cb.equal(root.get("status"), 1)
            ));
            for (Blacklist blacklist : blacklistList) {
                ObjectNode item = DPUtil.objectNode();
                item.put("id", blacklist.getId());
                item.put("domain", blacklist.getDomain());
                item.put("regexDomain", regexDomain(blacklist.getDomain()));
                item.put("path", blacklist.getPath());
                item.put("regexPath", regexPath(blacklist.getPath()));
                blacklists.replace(String.valueOf(blacklist.getId()), item);
            }
        }
        template.setContent(DPUtil.stringify(node));
        template.setPublishedTime(System.currentTimeMillis());
        template.setPublishedUid(rbacService.uid(request));
        templateDao.save(template);
        return nodeService.submit(node);
    }

    public String regexDomain(String domain) {
        domain = domain.replaceAll("\\.", "\\\\.");
        domain = domain.replaceAll("-", "\\\\-");
        domain = domain.replaceAll("\\*", ".*?");
        return "^" + domain + "$";
    }

    public String regexPath(String path) {
        path = path.replaceAll("\\.", "\\\\.");
        path = path.replaceAll("-", "\\\\-");
        path = path.replaceAll("/", "\\\\/");
        path = path.replaceAll("\\?", "\\\\?");
        path = path.replaceAll("\\*", ".*?");
        return "^" + path + "$";
    }

    public Map<String, Object> clear(Map<?, ?> param, HttpServletRequest request) {
        Template template = info(DPUtil.parseInt(param.get("id")));
        if (null == template || 1 != template.getStatus()) {
            return ApiUtil.result(1401, "模板不存在或已禁用", null);
        }
        boolean saved = nodeService.zookeeper().save("/runtime/job/" + template.getId(), null);
        if (!saved) {
            return ApiUtil.result(1501, "清理模板配置失败", template);
        }
        template.setContent("");
        template.setPublishedTime(0L);
        template.setPublishedUid(rbacService.uid(request));
        template = templateDao.save(template);
        return ApiUtil.result(0, null, template);
    }

}
