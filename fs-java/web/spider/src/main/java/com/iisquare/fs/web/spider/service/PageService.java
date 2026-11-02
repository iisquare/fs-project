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
import java.util.Locale;
import java.util.Map;

@Service
public class PageService extends JPAServiceBase {

    @Autowired
    PageDao pageDao;
    @Autowired
    TemplateService templateService;
    @Autowired
    DefaultRbacService rbacService;

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

    public Map<String, String> requestTypes() {
        Map<String, String> requestTypes = new LinkedHashMap<>();
        requestTypes.put("GET", "GET");
        requestTypes.put("POST", "POST");
        requestTypes.put("PUT", "PUT");
        requestTypes.put("PATCH", "PATCH");
        requestTypes.put("DELETE", "DELETE");
        return requestTypes;
    }

    public Map<String, String> contentTypes() {
        Map<String, String> contentTypes = new LinkedHashMap<>();
        contentTypes.put("none", "none");
        contentTypes.put("form-data", "form-data");
        contentTypes.put("x-www-form-urlencoded", "x-www-form-urlencoded");
        contentTypes.put("json", "json");
        contentTypes.put("xml", "xml");
        contentTypes.put("raw", "raw");
        return contentTypes;
    }

    public Map<String, String> fetchModes() {
        Map<String, String> modes = new LinkedHashMap<>();
        modes.put("http", "标准 HTTP");
        modes.put("browser", "无头浏览器");
        return modes;
    }

    public Map<String, String> browserWaitModes() {
        Map<String, String> modes = new LinkedHashMap<>();
        modes.put("domcontentloaded", "DOM 就绪");
        modes.put("load", "资源加载完成");
        modes.put("networkidle", "网络空闲");
        return modes;
    }

    public Page info(Integer id) {
        return info(pageDao, id);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(pageDao, rows, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if(DPUtil.empty(code)) return ApiUtil.result(1001, "编码不能为空", code);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1002, "名称不能为空", name);
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        if(DPUtil.empty(url)) return ApiUtil.result(1003, "链接地址不能为空", url);
        String requestType = DPUtil.trim(DPUtil.parseString(param.get("requestType"))).toUpperCase(Locale.ROOT);
        if(DPUtil.empty(requestType)) requestType = "GET";
        if(!requestTypes().containsKey(requestType)) {
            return ApiUtil.result(1005, "请求类型异常", requestType);
        }
        String contentType = DPUtil.trim(DPUtil.parseString(param.get("contentType")));
        if(DPUtil.empty(contentType)) contentType = "none";
        if(!contentTypes().containsKey(contentType)) {
            return ApiUtil.result(1006, "请求参数类型异常", contentType);
        }
        String fetchMode = DPUtil.trim(DPUtil.parseString(param.get("fetchMode"))).toLowerCase(Locale.ROOT);
        if (DPUtil.empty(fetchMode)) fetchMode = "http";
        if (!fetchModes().containsKey(fetchMode)) {
            return ApiUtil.result(1007, "页面请求模式异常", fetchMode);
        }
        String browserWaitUntil = DPUtil.trim(DPUtil.parseString(param.get("browserWaitUntil"))).toLowerCase(Locale.ROOT);
        if (DPUtil.empty(browserWaitUntil)) browserWaitUntil = "networkidle";
        if (!browserWaitModes().containsKey(browserWaitUntil)) {
            return ApiUtil.result(1008, "浏览器等待模式异常", browserWaitUntil);
        }
        int browserTimeout = DPUtil.parseInt(param.get("browserTimeout"));
        if (browserTimeout <= 0) browserTimeout = 30000;
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
        info.setRequestType(requestType);
        info.setRequestBody(DPUtil.parseString(param.get("requestBody")));
        info.setContentType(contentType);
        Object payloadForm = param.get("payloadForm");
        info.setPayloadForm(payloadForm instanceof String ? DPUtil.parseString(payloadForm) : DPUtil.stringify(payloadForm));
        info.setFetchMode(fetchMode);
        info.setBrowserWaitUntil(browserWaitUntil);
        info.setBrowserWaitSelector(DPUtil.parseString(param.get("browserWaitSelector")));
        info.setBrowserTimeout(browserTimeout);
        info.setCharset(DPUtil.parseString(param.get("charset")));
        info.setCollection(DPUtil.parseString(param.get("collection")));
        info.setBucket(DPUtil.parseString(param.get("bucket")));
        info.setConnectTimeout(DPUtil.parseInt(param.get("connectTimeout")));
        info.setSocketTimeout(DPUtil.parseInt(param.get("socketTimeout")));
        info.setIterateCount(DPUtil.parseInt(param.get("iterateCount")));
        info.setRetryCount(DPUtil.parseInt(param.get("retryCount")));
        Object headers = param.get("headers");
        info.setHeaders(headers instanceof String ? DPUtil.parseString(headers) : DPUtil.stringify(headers));
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
            node.put("forceMode", DPUtil.parseBoolean(node.at("/forceMode").asInt()));
            if(DPUtil.empty(node.at("/requestType").asText())) node.put("requestType", "GET");
            if(DPUtil.empty(node.at("/contentType").asText())) node.put("contentType", "none");
            if(DPUtil.empty(node.at("/fetchMode").asText())) node.put("fetchMode", "http");
            if(DPUtil.empty(node.at("/browserWaitUntil").asText())) node.put("browserWaitUntil", "networkidle");
            if(node.at("/browserTimeout").asInt(0) <= 0) node.put("browserTimeout", 30000);
            node.replace("headers", DPUtil.parseJSON(node.at("/headers").asText("{}"), j -> DPUtil.objectNode()));
            node.replace("payloadForm", DPUtil.parseJSON(node.at("/payloadForm").asText("{}"), j -> DPUtil.objectNode()));
        }
        return rows;
    }

    public Map<String, Object> remove(List<Integer> ids) {
        return remove(pageDao, ids);
    }

}
