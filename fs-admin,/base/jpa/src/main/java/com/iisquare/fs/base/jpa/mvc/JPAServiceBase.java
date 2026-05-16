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

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

public class JPAServiceBase extends ServiceBase {

    public JsonNode filter(JsonNode json) {
        return json;
    }

    protected <T> JsonNode fillInfo(DaoBase<T, Integer> dao, JsonNode rows, String... properties) {
        return fillInfo(dao, Integer.class, rows, properties);
    }

    protected <T, ID extends Serializable> JsonNode fillInfo(
            DaoBase<T, ID> dao, Class<ID> idClass, JsonNode rows, String... properties) {
        return fillInfo(dao, idClass, "id", "Id", "Info", rows, properties);
    }

    protected <T, ID extends Serializable> JsonNode fillInfo(
            DaoBase<T, ID> dao, Class<ID> idClass, String idField, String fromSuffix, String toSuffix, JsonNode rows, String... properties) {
        Set<ID> ids = DPUtil.values(rows, idClass, properties);
        if(ids.isEmpty()) return rows;
        ObjectNode data = DPUtil.json2object(filter(DPUtil.toJSON(dao.findAllById(ids), ArrayNode.class)), idField);
        return DPUtil.fillValues(rows, true, properties, DPUtil.suffix(properties, fromSuffix, toSuffix), data);
    }

    protected <T> JsonNode fillInfos(DaoBase<T, Integer> dao, JsonNode rows, String... properties) {
        return fillInfos(dao, Integer.class, rows, properties);
    }

    protected <T, ID extends Serializable> JsonNode fillInfos(
            DaoBase<T, ID> dao, Class<ID> idClass, JsonNode rows, String... properties) {
        return fillInfos(dao, idClass, "id", "Ids", "s", rows, properties);
    }

    protected <T, ID extends Serializable> JsonNode fillInfos(
            DaoBase<T, ID> dao, Class<ID> idClass, String idField, String fromSuffix, String toSuffix, JsonNode rows, String... properties) {
        Set<ID> ids = new HashSet<>();
        for (Object obj : DPUtil.values(rows, Object.class, properties)) {
            for (String string : DPUtil.parseStringList(obj)) {
                ids.add(DPUtil.toJSON(string, idClass));
            }
        }
        if(ids.isEmpty()) return rows;
        ObjectNode data = DPUtil.json2object(filter(DPUtil.toJSON(dao.findAllById(ids), ArrayNode.class)), idField);
        String[] suffixed = DPUtil.suffix(properties, fromSuffix, toSuffix);
        for (JsonNode row : rows) {
            for (int i = 0; i < properties.length; i++) {
                ArrayNode arr = ((ObjectNode) row).putArray(suffixed[i]);
                List<String> list = DPUtil.parseStringList(DPUtil.toJSON(row.get(properties[i]), Object.class));
                for (String id : list) {
                    JsonNode item = data.at("/" + id);
                    if (item.isNull() || item.isEmpty()) continue;
                    arr.add(item);
                }
            }
        }
        return rows;
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
        if(null == ids || ids.isEmpty()) return false;
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
        if(null == ids || ids.isEmpty()) return false;
        dao.deleteAllByIdInBatch(ids);
        return true;
    }

    protected <T, ID extends Serializable> long removeByParentId(DaoBase<T, ID> dao, String field, List<ID> ids) {
        if(null == ids || ids.isEmpty()) return 0;
        return dao.delete((Specification<T>) (root, query, cb) -> root.get(field).in(ids));
    }

    protected <T, ID extends Serializable> ObjectNode infoByIds(DaoBase<T, ID> dao, List<ID> ids) {
        if (null == ids || ids.isEmpty()) return DPUtil.objectNode();
        ArrayNode data = DPUtil.toJSON(dao.findAllById(ids), ArrayNode.class);
        return DPUtil.json2object(data, "id");
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

    public <T, ID extends Serializable> T info(DaoBase<T, ID> dao, ID id) {
        if(null == id) return null;
        if (id instanceof Integer) {
            if (DPUtil.parseInt(id) < 1) return null;
        } else if (id instanceof Long) {
            if (DPUtil.parseLong(id) < 1L) return null;
        }
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
        long createdTime = DPUtil.parseLong(ReflectUtil.getPropertyValue(info, "createdTime"));
        if((createdUid == null || Integer.valueOf(0).equals(createdUid) & createdTime < 1)) {
            ReflectUtil.setPropertyValue(info, "createdTime", new Class[]{Long.class}, new Object[]{time});
            ReflectUtil.setPropertyValue(info, "createdUid", new Class[]{Integer.class}, new Object[]{uid});
        }
        return dao.save(info);
    }

}
