package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.DatasetDao;
import com.iisquare.fs.web.bi.dao.SourceDao;
import com.iisquare.fs.web.bi.entity.Dataset;
import com.iisquare.fs.web.bi.entity.Source;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Service
public class DatasetService extends ServiceBase {

    @Autowired
    private DatasetDao datasetDao;
    @Autowired
    private SourceDao sourceDao;
    @Autowired
    private SparkService sparkService;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<String, Object> loadSource(Collection<Integer> sourceIds) {
        if (null == sourceIds || sourceIds.size() < 1) return ApiUtil.result(2001, "未配置任何数据源", sourceIds);
        ObjectNode result = DPUtil.objectNode();
        Map<Integer, Source> sources = DPUtil.list2map(sourceDao.findAllById(sourceIds), Integer.class, "id");
        for (Integer sourceId : sourceIds) {
            Source source = sources.get(sourceId);
            if (null == source || 1 != source.getStatus()) return ApiUtil.result(2002, "数据源暂不可用", sourceId);
            JsonNode options = DPUtil.parseJSON(source.getContent());
            if (null == options) return ApiUtil.result(2003, "解析数据源配置异常", sourceId);
            ObjectNode item = DPUtil.objectNode();
            item.put("id", sourceId).put("type", source.getType()).replace("options", options);
            result.replace(String.valueOf(sourceId), item);
        }
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> search(JsonNode preview, JsonNode query) {
        if (null == preview || !preview.isObject()) {
            return ApiUtil.result(1001, "配置信息异常", null);
        }
        ObjectNode options = (ObjectNode) preview;
        options.replace("query", null == query ? DPUtil.objectNode() : query);
        Map<String, Object> result = loadSource(DPUtil.values(preview.at("/relation/items"), Integer.class, "sourceId"));
        if (ApiUtil.failed(result)) return result;
        options.replace("sources", ApiUtil.data(result, ObjectNode.class));
        return sparkService.dataset(options);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Dataset> data = datasetDao.findAll((Specification<Dataset>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
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

    public Dataset info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Dataset> info = datasetDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Dataset save(Dataset info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return datasetDao.save(info);
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Dataset> list = datasetDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Dataset item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        datasetDao.saveAll(list);
        return true;
    }

}
