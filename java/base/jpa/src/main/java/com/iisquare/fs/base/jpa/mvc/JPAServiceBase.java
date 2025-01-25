package com.iisquare.fs.base.jpa.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.*;

public class JPAServiceBase extends ServiceBase {

    public JsonNode filter(JsonNode json) {
        return json;
    }

    protected <T> JsonNode fillInfo(DaoBase<T, Integer> dao, JsonNode json, String... properties) {
        return fillInfo(dao, Integer.class, json, properties);
    }

    protected <T, ID extends Serializable> JsonNode fillInfo(
            DaoBase<T, ID> dao, Class<ID> idClass, JsonNode json, String... properties) {
        return fillInfo(dao, idClass, "id", "Id", "Info", json, properties);
    }

    protected <T, ID extends Serializable> JsonNode fillInfo(
            DaoBase<T, ID> dao, Class<ID> idClass, String idField, String fromSuffix, String toSuffix, JsonNode json, String... properties) {
        Set<ID> ids = DPUtil.values(json, idClass, properties);
        if(ids.size() < 1) return json;
        ObjectNode data = DPUtil.array2object(filter(DPUtil.toJSON(dao.findAllById(ids), ArrayNode.class)), idField);
        return DPUtil.fillValues(json, true, properties, DPUtil.suffix(properties, fromSuffix, toSuffix), data);
    }

    protected JsonNode fillStatus(JsonNode rows, Map<?, ?> map) {
        if (null == rows) return null;
        for (JsonNode row : rows) {
            ObjectNode item = (ObjectNode) row;
            long deletedTime = item.at("/deletedTime").asLong(0);
            if (deletedTime > 0) {
                item.put("statusText", "已删除");
                continue;
            }
            Object status = DPUtil.toJSON(item.at("/status"), Object.class);
            if (map.containsKey(status)) {
                item.replace("statusText", DPUtil.toJSON(map.get(status)));
            }
        }
        return rows;
    }

    protected <T, ID extends Serializable> boolean delete(DaoBase<T, ID> dao, List<ID> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<T> list = dao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (T item : list) {
            ReflectUtil.setPropertyValue(item, "deletedTime", new Class[]{Long.class}, new Object[]{time});
            ReflectUtil.setPropertyValue(item, "deletedUid", new Class[]{Integer.class}, new Object[]{uid});
        }
        dao.saveAll(list);
        return true;
    }

    protected <T, ID extends Serializable> boolean remove(DaoBase<T, ID> dao, List<ID> ids) {
        if(null == ids || ids.size() < 1) return false;
        dao.deleteInBatch(dao.findAllById(ids));
        return true;
    }

    protected <T, ID extends Serializable> ObjectNode infoByIds(DaoBase<T, ID> dao, List<ID> ids) {
        if (null == ids || ids.size() < 1) return DPUtil.objectNode();
        ArrayNode data = DPUtil.toJSON(dao.findAllById(ids), ArrayNode.class);
        return DPUtil.array2object(data, "id");
    }

    protected <T, ID extends Serializable> ObjectNode search(
            DaoBase<T, ID> dao, Map<String, Object> param, Specification<T> specification) {
        return search(dao, param, specification, 15, null, null);
    }

    protected <T, ID extends Serializable> ObjectNode search(
            DaoBase<T, ID> dao, Map<String, Object> param, Specification<T> specification, Sort defaultSort, String... sorts) {
        return search(dao, param, specification, 15, DPUtil.array2list(sorts), defaultSort);
    }

    protected <T, ID extends Serializable> ObjectNode search(
            DaoBase<T, ID> dao, Map<String, Object> param,
            Specification<T> specification, int defaultPageSize, Collection<String> sorts, Sort defaultSort) {
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, defaultPageSize);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), sorts);
        if (null == sort) sort = null == defaultSort ? Sort.unsorted() : defaultSort;
        Page<T> data = dao.findAll(specification, PageRequest.of(page - 1, pageSize, sort));
        ArrayNode rows = DPUtil.toJSON(data.getContent(), ArrayNode.class);
        ObjectNode result = DPUtil.objectNode();
        result.put(ApiUtil.FIELD_DATA_PAGE, page)
                .put(ApiUtil.FIELD_DATA_PAGE_SIZE, pageSize)
                .put(ApiUtil.FIELD_DATA_TOTAL, data.getTotalElements())
                .replace(ApiUtil.FIELD_DATA_ROWS, rows);
        return result;
    }

    public <T> T info(DaoBase<T, Integer> dao, Integer id) {
        if(null == id || id < 1) return null;
        Optional<T> info = dao.findById(id);
        return info.orElse(null);
    }

    public <T, ID extends Serializable> T save(DaoBase<T, ID> dao, T info, int uid) {
        long time = System.currentTimeMillis();
        if(uid > 0) {
            ReflectUtil.setPropertyValue(info, "updatedTime", new Class[]{Long.class}, new Object[]{time});
            ReflectUtil.setPropertyValue(info, "updatedUid", new Class[]{Integer.class}, new Object[]{uid});
        }
        Object createdUid = ReflectUtil.getPropertyValue(info, "createdUid");
        if(createdUid == null || Integer.valueOf(0).equals(createdUid)) {
            ReflectUtil.setPropertyValue(info, "createdTime", new Class[]{Long.class}, new Object[]{time});
            ReflectUtil.setPropertyValue(info, "createdUid", new Class[]{Integer.class}, new Object[]{uid});
        }
        return dao.save(info);
    }

}
