package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.bi.dao.DataApiDao;
import com.iisquare.fs.web.bi.datasource.DatasourceConnector;
import com.iisquare.fs.web.bi.entity.DataApi;
import com.iisquare.fs.web.bi.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataApiService extends JPAServiceBase {

    @Autowired
    DataApiDao dataApiDao;
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

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> methods() {
        Map<String, String> methods = new LinkedHashMap<>();
        methods.put("get", "GET");
        methods.put("post", "POST");
        return methods;
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

    public DataApi info(Integer id) {
        return info(dataApiDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称不能为空", name);
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        if(DPUtil.empty(url)) return ApiUtil.result(1002, "接口地址不能为空", url);
        String method = DPUtil.trim(DPUtil.parseString(param.get("method")));
        if(!methods().containsKey(method)) return ApiUtil.result(1003, "请求方式异常", method);
        String contentType = DPUtil.trim(DPUtil.parseString(param.get("contentType")));
        if(!DPUtil.empty(contentType) && !contentTypes().containsKey(contentType)) {
            return ApiUtil.result(1004, "请求体类型异常", contentType);
        }
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1005, "状态异常", status);
        DataApi info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new DataApi();
        }
        int count = dataApiDao.exist(name, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1501, "名称已存在", name);
        }
        info.setName(name);
        info.setUrl(url);
        info.setMethod(method);
        info.setTimeout(DPUtil.parseInt(param.get("timeout")));
        info.setHeaders(DPUtil.stringify(param.get("headers")));
        info.setContentType(contentType);
        info.setPayloadForm(DPUtil.stringify(param.get("payloadForm")));
        info.setPayloadBody(DPUtil.parseString(param.get("payloadBody")));
        info.setPks(DPUtil.implode(",", DPUtil.parseStringList(param.get("pks"))));
        info.setPageRequestField(DPUtil.trim(DPUtil.parseString(param.get("pageRequestField"))));
        info.setPageSizeRequestField(DPUtil.trim(DPUtil.parseString(param.get("pageSizeRequestField"))));
        info.setPageResponseField(DPUtil.trim(DPUtil.parseString(param.get("pageResponseField"))));
        info.setPageSizeResponseField(DPUtil.trim(DPUtil.parseString(param.get("pageSizeResponseField"))));
        info.setTotalResponseField(DPUtil.trim(DPUtil.parseString(param.get("totalResponseField"))));
        info.setFields(DPUtil.stringify(param.get("fields")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(dataApiDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataApiDao, param, (root, query, cb) -> {
            SpecificationHelper<DataApi> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").like("url").equal("method");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
            DPUtil.fillValues(rows, "method", "methodText", methods());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<String> pks = DPUtil.parseStringList(node.at("/pks").asText(""));
            node.replace("pks", DPUtil.toJSON(pks));
            node.replace("fields", DPUtil.parseJSON(node.at("/fields").asText("[]")));
            node.replace("headers", DPUtil.parseJSON(node.at("/headers").asText("{}")));
            node.replace("payloadForm", DPUtil.parseJSON(node.at("/payloadForm").asText("{}")));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(dataApiDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(dataApiDao, rows, properties);
    }

    public JsonNode fillInfos(JsonNode rows, String ...properties) {
        return fillInfos(dataApiDao, rows, properties);
    }

    public Map<String, Object> test(JsonNode config, HttpServletRequest request) {
        DatasourceConnector connector = DatasourceConnector.connector("http", config);
        return connector.test();
    }

}
