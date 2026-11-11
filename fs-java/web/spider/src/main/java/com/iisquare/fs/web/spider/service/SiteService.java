package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.BlacklistDao;
import com.iisquare.fs.web.spider.dao.SiteDao;
import com.iisquare.fs.web.spider.dao.WhitelistDao;
import com.iisquare.fs.web.spider.entity.Blacklist;
import com.iisquare.fs.web.spider.entity.Site;
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
import java.util.Locale;
import java.util.Map;

@Service
public class SiteService extends JPAServiceBase {

    @Autowired
    SiteDao siteDao;
    @Autowired
    TemplateService templateService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    PageService pageService;
    @Autowired
    WhitelistDao whitelistDao;
    @Autowired
    BlacklistDao blacklistDao;
    @Autowired
    NodeService nodeService;

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
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> browserWaitModes() {
        return pageService.browserWaitModes();
    }

    public Map<String, String> fetchModes() {
        return pageService.fetchModes();
    }

    public Site info(Integer id) {
        return info(siteDao, id);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(siteDao, rows, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
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
        String fetchMode = DPUtil.trim(DPUtil.parseString(param.get("fetchMode"))).toLowerCase(Locale.ROOT);
        if (DPUtil.empty(fetchMode)) fetchMode = "http";
        if (!pageService.fetchModes().containsKey(fetchMode)) {
            return ApiUtil.result(1004, "页面请求模式异常", fetchMode);
        }
        String browserWaitUntil = DPUtil.trim(
                DPUtil.parseString(param.get("browserWaitUntil"))).toLowerCase(Locale.ROOT);
        if (DPUtil.empty(browserWaitUntil)) browserWaitUntil = "networkidle";
        if (!browserWaitModes().containsKey(browserWaitUntil)) {
            return ApiUtil.result(1004, "浏览器页面就绪条件异常", browserWaitUntil);
        }
        int browserTimeout = DPUtil.parseInt(param.get("browserTimeout"));
        if (browserTimeout <= 0) browserTimeout = 30000;
        info.setFetchMode(fetchMode);
        info.setBrowserWaitUntil(browserWaitUntil);
        info.setBrowserWaitSelector(DPUtil.parseString(param.get("browserWaitSelector")));
        info.setBrowserTimeout(browserTimeout);
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
                predicates.add(cb.like(root.get("name"), name));
            }
            String domain = DPUtil.trim(DPUtil.parseString(param.get("domain")));
            if (!DPUtil.empty(domain)) {
                predicates.add(cb.like(root.get("domain"), domain));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
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
            if (DPUtil.empty(node.at("/fetchMode").asText())) node.put("fetchMode", "http");
            if (DPUtil.empty(node.at("/browserWaitUntil").asText())) node.put("browserWaitUntil", "networkidle");
            if (node.at("/browserTimeout").asInt(0) <= 0) node.put("browserTimeout", 30000);
        }
        return rows;
    }

    public Map<String, Object> remove(List<Integer> ids) {
        return remove(siteDao, ids);
    }

    /**
     * 接收 site.backend 的网站入口与访问规则，先同步到指定模板，再发布并启动采集作业。
     */
    public Map<String, Object> sync(Map<?, ?> param, HttpServletRequest request) {
        int templateId = DPUtil.parseInt(param.get("templateId"));
        Template template = templateService.info(templateId);
        if (null == template || !Integer.valueOf(1).equals(template.getStatus())) {
            return ApiUtil.result(1401, "采集模板不存在或已禁用", templateId);
        }
        List<?> sites = param.get("sites") instanceof List<?> list ? list : List.of();
        List<?> whitelists = param.get("whitelists") instanceof List<?> list ? list : List.of();
        List<?> blacklists = param.get("blacklists") instanceof List<?> list ? list : List.of();
        if (sites.isEmpty() && whitelists.isEmpty()) {
            return ApiUtil.result(1402, "网站入口与白名单不能同时为空", templateId);
        }
        int siteCount = syncSites(templateId, sites);
        int whitelistCount = syncWhitelists(templateId, whitelists);
        int blacklistCount = syncBlacklists(templateId, blacklists);
        Map<String, Object> published = templateService.publish(DPUtil.buildMap("id", templateId), request);
        if (ApiUtil.failed(published)) return published;
        Map<String, Object> job = null;
        if (!param.containsKey("start") || DPUtil.parseBoolean(param.get("start"))) {
            ObjectNode jobParam = DPUtil.objectNode();
            jobParam.put("id", templateId);
            job = nodeService.startJob(jobParam);
            if (ApiUtil.failed(job)) return job;
        }
        ObjectNode data = DPUtil.objectNode();
        data.put("templateId", templateId)
                .put("sites", siteCount)
                .put("whitelists", whitelistCount)
                .put("blacklists", blacklistCount);
        JsonNode publishedData = DPUtil.toJSON(published.get("data"));
        data.set("published", publishedData);
        data.set("job", null == job ? DPUtil.objectNode() : DPUtil.toJSON(job.get("data")));
        return ApiUtil.result(0, null, data);
    }

    private int syncSites(int templateId, List<?> sites) {
        int count = 0;
        Map<String, Site> existing = new LinkedHashMap<>();
        for (Site site : siteDao.findAll((Specification<Site>) (root, query, cb) ->
                cb.equal(root.get("templateId"), templateId))) {
            existing.put(site.getDomain(), site);
        }
        for (Object raw : sites) {
            if (!(raw instanceof Map<?, ?> item)) continue;
            String domain = DPUtil.trim(DPUtil.parseString(item.get("domain")));
            if (DPUtil.empty(domain)) continue;
            Site site = existing.get(domain);
            if (null == site) {
                site = new Site();
                site.setTemplateId(templateId);
                site.setDomain(domain);
                site.setStatus(1);
                existing.put(domain, site);
            }
            site.setName(DPUtil.parseString(item.get("name")));
            site.setDomain(domain);
            String charset = DPUtil.parseString(item.get("charset"));
            site.setCharset(DPUtil.empty(charset) ? "utf-8" : charset);
            site.setCollection(DPUtil.parseString(item.get("collection")));
            site.setRetryCount(DPUtil.parseInt(item.get("retryCount")));
            site.setRetainQuery(0);
            site.setRetainAnchor(0);
            site.setConnectTimeout(0);
            site.setSocketTimeout(0);
            site.setIterateCount(0);
            site.setFetchMode("http");
            site.setBrowserWaitUntil("networkidle");
            site.setBrowserTimeout(30000);
            save(siteDao, site, 0);
            count++;
        }
        return count;
    }

    private int syncWhitelists(int templateId, List<?> rules) {
        int count = 0;
        Map<String, Whitelist> existing = new LinkedHashMap<>();
        for (Whitelist rule : whitelistDao.findAll((Specification<Whitelist>) (root, query, cb) ->
                cb.equal(root.get("templateId"), templateId))) {
            existing.put(rule.getDomain() + "|" + rule.getPath(), rule);
        }
        for (Object raw : rules) {
            if (!(raw instanceof Map<?, ?> item)) continue;
            String domain = DPUtil.trim(DPUtil.parseString(item.get("domain")));
            String path = DPUtil.trim(DPUtil.parseString(item.get("path")));
            if (DPUtil.empty(domain)) continue;
            if (DPUtil.empty(path)) path = "*";
            String key = domain + "|" + path;
            Whitelist rule = existing.get(key);
            if (null == rule) {
                rule = new Whitelist();
                rule.setTemplateId(templateId);
                rule.setDomain(domain);
                rule.setPath(path);
                rule.setStatus(1);
                existing.put(key, rule);
            }
            rule.setName(DPUtil.parseString(item.get("name")));
            save(whitelistDao, rule, 0);
            count++;
        }
        return count;
    }

    private int syncBlacklists(int templateId, List<?> rules) {
        int count = 0;
        Map<String, Blacklist> existing = new LinkedHashMap<>();
        for (Blacklist rule : blacklistDao.findAll((Specification<Blacklist>) (root, query, cb) ->
                cb.equal(root.get("templateId"), templateId))) {
            existing.put(rule.getDomain() + "|" + rule.getPath(), rule);
        }
        for (Object raw : rules) {
            if (!(raw instanceof Map<?, ?> item)) continue;
            String domain = DPUtil.trim(DPUtil.parseString(item.get("domain")));
            String path = DPUtil.trim(DPUtil.parseString(item.get("path")));
            if (DPUtil.empty(domain)) continue;
            if (DPUtil.empty(path)) path = "*";
            String key = domain + "|" + path;
            Blacklist rule = existing.get(key);
            if (null == rule) {
                rule = new Blacklist();
                rule.setTemplateId(templateId);
                rule.setDomain(domain);
                rule.setPath(path);
                rule.setStatus(1);
                existing.put(key, rule);
            }
            rule.setName(DPUtil.parseString(item.get("name")));
            save(blacklistDao, rule, 0);
            count++;
        }
        return count;
    }

}
