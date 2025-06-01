package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.DataDao;
import com.iisquare.fs.web.member.dao.DataPermitDao;
import com.iisquare.fs.web.member.entity.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class DataService extends JPAServiceBase {

    @Autowired
    private DataDao dataDao;
    @Autowired
    private DataPermitDao dataPermitDao;
    @Autowired
    private UserService userService;
    @Autowired
    private RbacService rbacService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public List<String> types() {
        return Arrays.asList(
                "string",
                "integer",
                "long",
                "float",
                "double",
                "date",
                "time",
                "datetime"
        );
    }

    public Data info(Integer id) {
        return info(dataDao, id);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(dataDao, json, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial)) return ApiUtil.result(1001, "标识异常", serial);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1002, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Data info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Data();
        }
        int count = dataDao.exist(serial, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1003, "标识已存在", serial);
        }
        info.setSerial(serial);
        info.setName(name);
        info.setPks(DPUtil.implode(DPUtil.parseStringList(param.get("pks"))));
        info.setFields(DPUtil.stringify(param.get("fields")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(dataDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataDao, param, (Specification<Data>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
            if (!DPUtil.empty(serial)) {
                predicates.add(cb.like(root.get("serial"), "%" + serial + "%"));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<String> pks = DPUtil.parseStringList(node.at("/pks").asText(""));
            node.replace("pks", DPUtil.toJSON(pks));
            node.replace("fields", DPUtil.parseJSON(node.at("/fields").asText("[]")));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.isEmpty()) return false;
        dataPermitDao.deleteByDataIds(ids);
        dataDao.deleteInBatch(dataDao.findAllById(ids));
        return true;
    }

}
