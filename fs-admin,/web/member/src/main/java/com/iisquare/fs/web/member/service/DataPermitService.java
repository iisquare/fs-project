package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.DataPermitDao;
import com.iisquare.fs.web.member.entity.DataPermit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class DataPermitService extends JPAServiceBase {

    @Autowired
    private DataPermitDao dataPermitDao;
    @Autowired
    private UserService userService;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private DataService dataService;
    @Autowired
    private RoleService roleService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public DataPermit info(Integer id) {
        return info(dataPermitDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        DataPermit info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new DataPermit();
        }
        info.setDataId(DPUtil.parseInt(param.get("dataId")));
        info.setRoleId(DPUtil.parseInt(param.get("roleId")));
        info.setFilters(DPUtil.stringify(param.get("filters")));
        info.setFields(DPUtil.implode(DPUtil.parseStringList(param.get("fields"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(dataPermitDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataPermitDao, param, (Specification<DataPermit>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int dataId = DPUtil.parseInt(param.get("dataId"));
            if (dataId > 0) predicates.add(cb.equal(root.get("dataId"), dataId));
            int roleId = DPUtil.parseInt(param.get("roleId"));
            if (roleId > 0) predicates.add(cb.equal(root.get("roleId"), roleId));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withDataInfo"))) {
            dataService.fillInfo(rows, "dataId");
        }
        if (!DPUtil.empty(args.get("withRoleInfo"))) {
            roleService.fillInfo(rows, "roleId");
        }
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
            List<String> fields = DPUtil.parseStringList(node.at("/fields").asText(""));
            node.replace("fields", DPUtil.toJSON(fields));
            node.replace("filters", DPUtil.parseJSON(node.at("/filters").asText("[]")));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(dataPermitDao, ids);
    }

}
