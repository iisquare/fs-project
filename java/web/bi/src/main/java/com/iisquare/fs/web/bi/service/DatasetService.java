package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import javax.servlet.http.HttpServletRequest;
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

    public Map<String, Object> loadSource(JsonNode preview) {
        if (null == preview) return ApiUtil.result(20403, "数据源配置信息异常", null);
        Collection<Integer> sourceIds = DPUtil.values(preview.at("/relation/items"), Integer.class, "sourceId");
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

    public Map<String, Object> dataset(Integer id) {
        Dataset info = info(id);
        if (null == info || 1 != info.getStatus()) {
            return ApiUtil.result(61001, "数据集状态异常", id);
        }
        JsonNode dataset = DPUtil.parseJSON(info.getContent());
        Map<String, Object> result = loadSource(dataset);
        if (ApiUtil.failed(result)) return result;
        ((ObjectNode) dataset).replace("sources", ApiUtil.data(result, ObjectNode.class));
        return ApiUtil.result(0, null, dataset);
    }

    public Map<String, Object> search(JsonNode preview, JsonNode query) {
        if (null == preview || !preview.isObject()) {
            return ApiUtil.result(1001, "配置信息异常", null);
        }
        ObjectNode options = (ObjectNode) preview;
        options.replace("query", null == query ? DPUtil.objectNode() : query);
        Map<String, Object> result = loadSource(preview);
        if (ApiUtil.failed(result)) return result;
        options.replace("sources", ApiUtil.data(result, ObjectNode.class));
        return sparkService.dataset(options);
    }

    public Map<String, Object> columns(JsonNode options) {
        if (null == options || !options.isObject()) {
            return ApiUtil.result(1001, "配置信息异常", null);
        }
        ObjectNode result = DPUtil.objectNode();
        ArrayNode columns = result.putArray("columns");
        Iterator<JsonNode> iterator = options.at("/table").iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (!item.at("/enabled").asBoolean(false)) continue;
            ObjectNode column = (ObjectNode) item.deepCopy();
            column.remove("enabled");
            columns.add(column);
        }
        return ApiUtil.result(0, null, result);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Dataset> data = datasetDao.findAll((Specification<Dataset>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if(id > 0) predicates.add(cb.equal(root.get("id"), id));
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

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        String collection = DPUtil.trim(DPUtil.parseString(param.get("collection")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        if(param.containsKey("status")) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        Dataset info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Dataset();
        }
        if(param.containsKey("collection") || null == info.getId()) info.setCollection(collection);
        if(param.containsKey("name") || null == info.getId()) info.setName(name);
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
        info = datasetDao.save(info);
        return ApiUtil.result(0, null, info);

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

    public <T> List<T> fillInfo(List<T> list, String ...properties) {
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, Dataset> data = DPUtil.list2map(datasetDao.findAllById(ids), Integer.class, "id");
        return DPUtil.fillValues(list, properties, "Name", DPUtil.values(data, String.class, "name"));
    }

}
