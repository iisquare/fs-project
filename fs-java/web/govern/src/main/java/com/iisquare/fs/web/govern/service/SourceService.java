package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.dao.SourceDao;
import com.iisquare.fs.web.govern.entity.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class SourceService extends ServiceBase {

    @Autowired
    private SourceDao sourceDao;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Source> data = sourceDao.findAll((Specification<Source>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
            if(!DPUtil.empty(code)) {
                predicates.add(cb.like(root.get("code"), code));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), name));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            String version = DPUtil.trim(DPUtil.parseString(param.get("version")));
            if(!DPUtil.empty(version)) {
                predicates.add(cb.like(root.get("version"), version));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public Source info(String code) {
        Optional<Source> info = sourceDao.findById(code);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> info(Map<?, ?> param) {
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1002, "数据源编码不能为空", code);
        Source info = info(code);
        if (null == info) return ApiUtil.result(1404, "数据源信息不存在", null);
        ObjectNode result = DPUtil.toJSON(info, ObjectNode.class);
        JsonNode content = DPUtil.parseJSON(info.getContent());
        result.replace("content", null == content ? DPUtil.objectNode(): content);
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1001, "数据源编码不能为空", code);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1005, "数据源名称不能为空", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(param.containsKey("status")) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        boolean isNew = true;
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        Source info = info(code);
        if (null == info) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = Source.builder().code(code)
                    .createdUid(uid).updatedUid(uid).createdTime(time).updatedTime(time).build();
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            isNew = false;
            info.setUpdatedUid(uid);
            info.setUpdatedTime(time);
        }
        if (isNew || param.containsKey("name")) info.setName(name);
        if (isNew || param.containsKey("status")) info.setStatus(status);
        if (isNew || param.containsKey("type")) info.setType(DPUtil.trim(DPUtil.parseString(param.get("type"))));
        if (isNew || param.containsKey("version")) info.setVersion(DPUtil.trim(DPUtil.parseString(param.get("version"))));
        if (isNew || param.containsKey("sort")) info.setSort(DPUtil.parseInt(param.get("sort")));
        if (isNew || param.containsKey("content")) info.setContent(DPUtil.stringify(param.get("content")));
        if (isNew || param.containsKey("description")) info.setDescription(DPUtil.parseString(param.get("description")));
        sourceDao.save(info);
        return info(DPUtil.buildMap("code", code));
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1001, "数据源编码不能为空", code);
        if(!rbacService.hasPermit(request, "delete")) return ApiUtil.result(9403, null, null);
        Source info = info(code);
        if (null == info) return ApiUtil.result(0, null, null);
        sourceDao.delete(info);
        return ApiUtil.result(0, null, info);
    }

}
