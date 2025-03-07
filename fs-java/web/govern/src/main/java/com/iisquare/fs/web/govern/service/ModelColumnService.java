package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.govern.dao.ModelColumnDao;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.ModelColumn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.sql.JDBCType;
import java.util.*;

@Service
public class ModelColumnService extends ServiceBase {

    @Autowired
    private ModelColumnDao columnDao;

    public List<String> types() {
        ArrayList<String> result = new ArrayList<>(Arrays.asList(
                "INT",
                "TEXT"
        ));
        result.addAll(DPUtil.parseStringList(JDBCType.values()));
        return result;
    }

    public Map<String, Object> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("catalog", "model", "code", "type", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.asc("sort"));
        Page<ModelColumn> data = columnDao.findAll((Specification<ModelColumn>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
            if(!DPUtil.empty(catalog)) {
                predicates.add(cb.equal(root.get("catalog"), catalog));
            }
            String model = DPUtil.trim(DPUtil.parseString(param.get("model")));
            if(!DPUtil.empty(model)) {
                predicates.add(cb.equal(root.get("model"), model));
            }
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
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, sort));
        List<ModelColumn> rows = data.getContent();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public ArrayNode format(List<ModelColumn> columns) {
        ArrayNode result = DPUtil.toJSON(columns, ArrayNode.class);
        Iterator<JsonNode> iterator = result.iterator();
        while (iterator.hasNext()) {
            ObjectNode item = (ObjectNode) iterator.next();
            item.put("nullable", DPUtil.parseBoolean(item.at("/nullable").asInt(0)));
        }
        return result;
    }

    public Map<String, Object> save(String catalog, String model, JsonNode columns) {
        Integer result = columnDao.deleteByCatalogAndModel(catalog, model);
        if (null == columns || columns.size() < 1) {
            return ApiUtil.result(0, null, result);
        }
        Map<String, ModelColumn> data = new LinkedHashMap();
        Iterator<JsonNode> iterator = columns.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            index++;
            JsonNode item = iterator.next();
            ModelColumn.ModelColumnBuilder builder = ModelColumn.builder();
            builder.catalog(catalog).model(model);
            String code = item.at("/code").asText();
            if (DPUtil.empty(code)) {
                return ApiUtil.result(31001, "第" + index + "行的字段编码为空", index);
            }
            if (!Model.safeCode(code)) {
                return ApiUtil.result(31011, "第" + index + "行的字段格式异常", index);
            }
            builder.code(code).sort(index);
            builder.name(item.at("/name").asText(""));
            String type = item.at("/type").asText();
            if (DPUtil.empty(type)) {
                return ApiUtil.result(31002, "第" + index + "行的字段类型为空", index);
            }
            builder.type(type).size(item.at("/size").asInt(0));
            builder.digit(item.at("/digit").asInt(0));
            builder.nullable(item.at("/nullable").asBoolean(false) ? 1 : 0);
            builder.description(item.at("/description").asText(""));
            data.put(code, builder.build());
        }
        if (data.size() != columns.size()) {
            return ApiUtil.result(35001, "字段编码不能重复", data);
        }
        List<ModelColumn> list = columnDao.saveAll(data.values());
        return ApiUtil.result(0, null, list);
    }

}
