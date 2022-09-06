package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.VisualizeDao;
import com.iisquare.fs.web.bi.entity.Dataset;
import com.iisquare.fs.web.bi.entity.Visualize;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
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
public class VisualizeService extends ServiceBase {

    @Autowired
    private VisualizeDao visualizeDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private SparkService sparkService;

    public Map<String, Object> search(Integer datasetId, JsonNode preview, JsonNode level) {
        if (null == preview || !preview.isObject()) {
            return ApiUtil.result(1001, "配置信息异常", null);
        }
        Map<String, Object> result = datasetService.dataset(datasetId);
        if (ApiUtil.failed(result)) return result;
        ObjectNode dataset = ApiUtil.data(result, ObjectNode.class);
        return sparkService.visualize(dataset, preview, level);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Visualize> data = visualizeDao.findAll((Specification<Visualize>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            int datasetId = DPUtil.parseInt(param.get("datasetId"));
            if(!"".equals(DPUtil.parseString(param.get("datasetId")))) {
                predicates.add(cb.equal(root.get("datasetId"), datasetId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withDatasetInfo"))) {
            datasetService.fillInfo(rows, "datasetId");
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

    public Visualize info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Visualize> info = visualizeDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        int datasetId = DPUtil.parseInt(param.get("datasetId"));
        String type = DPUtil.parseString(param.get("type"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        if(param.containsKey("status")) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        Visualize info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Visualize();
        }
        if(param.containsKey("datasetId") || null == info.getId()) info.setDatasetId(datasetId);
        if(param.containsKey("name") || null == info.getId()) info.setName(name);
        if(param.containsKey("type") || null == info.getId()) info.setType(type);
        if(param.containsKey("content") || null == info.getId()) info.setContent(content);
        if(param.containsKey("description") || null == info.getId()) info.setDescription(description);
        if(param.containsKey("sort") || null == info.getId()) info.setSort(sort);
        if(param.containsKey("status") || null == info.getId()) info.setStatus(status);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = visualizeDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Visualize> list = visualizeDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Visualize item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        visualizeDao.saveAll(list);
        return true;
    }

}
