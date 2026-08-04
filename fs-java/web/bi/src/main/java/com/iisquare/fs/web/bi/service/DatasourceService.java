package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.bi.dao.DatasourceDao;
import com.iisquare.fs.web.bi.datasource.DatasourceConnector;
import com.iisquare.fs.web.bi.entity.Datasource;
import com.iisquare.fs.web.bi.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class DatasourceService extends JPAServiceBase {

    @Autowired
    DatasourceDao datasourceDao;
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
        types.put("mysql", "MySQL");
        types.put("doris", "Apache Doris");
        return types;
    }

    public Datasource info(Integer id) {
        return info(datasourceDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "数据源名称不能为空", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(!types().containsKey(type)) return ApiUtil.result(1002, "数据源类型异常", type);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1005, "状态异常", status);
        Datasource info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Datasource();
        }
        info.setName(name);
        info.setType(type);
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(datasourceDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(datasourceDao, param, (root, query, cb) -> {
            SpecificationHelper<Datasource> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").equal("type");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
            DPUtil.fillValues(rows, "type", "typeText", types());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        Map<String, String> types = types();
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            JsonNode content = DPUtil.parseJSON(node.at("/content").asText(), k -> DPUtil.objectNode());
            node.replace("content", content);
            DatasourceConnector connector = DatasourceConnector.connector(node.at("/type").asText(), content);
            node.put("summary", connector.summary());
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(datasourceDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(datasourceDao, rows, properties);
    }

    public JsonNode fillInfos(JsonNode rows, String ...properties) {
        return fillInfos(datasourceDao, rows, properties);
    }

}
