package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.web.member.dao.DictionaryDao;
import com.iisquare.fs.web.member.entity.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class DictionaryService extends JPAServiceBase {

    @Autowired
    private DictionaryDao dictionaryDao;
    @Autowired
    private UserService userService;
    @Autowired
    private RbacService rbacService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Dictionary info(Integer id) {
        return info(dictionaryDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Dictionary info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Dictionary();
        }
        info.setName(name);
        info.setPinyin(DPUtil.trim(DPUtil.parseString(param.get("pinyin"))));
        info.setAncestorId(DPUtil.parseInt(param.get("ancestorId")));
        info.setParentId(DPUtil.parseInt(param.get("parentId")));
        info.setContent(DPUtil.trim(DPUtil.parseString(param.get("content"))));
        info.setLeaf(DPUtil.parseBoolean(param.get("leaf")) ? 1 : 0);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(dictionaryDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(dictionaryDao, json, properties);
    }

    public ArrayNode formatOptions(ArrayNode list) {
        ArrayNode result = DPUtil.arrayNode();
        for (JsonNode dictionary : list) {
            String name = dictionary.at("/name").asText();
            String content = dictionary.at("/content").asText();
            if (DPUtil.empty(content)) {
                content = String.valueOf(dictionary.at("/id").asInt());
            }
            ObjectNode node = result.addObject();
            node.replace("id", dictionary.at("/id"));
            node.replace("parentId", dictionary.at("/parentId"));
            node.put("label", name);
            node.put("value", content);
            node.replace("leaf", dictionary.at("/leaf"));
            JsonNode children = dictionary.at("/children");
            if (!children.isArray() || children.isEmpty()) continue;
            node.replace("children", formatOptions((ArrayNode) children));
        }
        return result;
    }

    public ArrayNode options(Map<?, ?> param) {
        ArrayNode result = DPUtil.arrayNode();
        String dictionary = DPUtil.parseString(param.get("dictionary"));
        Dictionary ancestor = dictionaryDao.findOne((Specification<Dictionary>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ancestorId"), 0));
            predicates.add(cb.equal(root.get("content"), dictionary));
            return cb.and(predicates.toArray(new Predicate[0]));
        }).orElse(null);
        if (null == ancestor || 1 != ancestor.getStatus()) return result;
        String parentId = DPUtil.parseString(param.get("parentId")); // 若非空，则为级联选择中的单一层级
        ArrayNode tree = tree(DPUtil.buildMap(
                "ancestorId", ancestor.getId(), "parentId", parentId, "status", 1, "sort", param.get("sort")
        ), DPUtil.buildMap());
        result = formatOptions(tree);
        return result;
    }

    public ArrayNode tree(Map<?, ?> param, Map<?, ?> args) {
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "name", "pinyin", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("pinyin"));
        List<Dictionary> list = dictionaryDao.findAll((Specification<Dictionary>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int ancestorId = DPUtil.parseInt(param.get("ancestorId"));
            // 为空时为所有字典列表，非空时为单个字典的所有字典项
            predicates.add(cb.equal(root.get("ancestorId"), ancestorId));
            int parentId = DPUtil.parseInt(param.get("parentId"));
            if (!"".equals(DPUtil.parseString(param.get("parentId")))) {
                predicates.add(cb.equal(root.get("parentId"), parentId));
            }
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, sort);
        ArrayNode data = DPUtil.toJSON(list, ArrayNode.class);
        format(data);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(data, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(data, status());
        }
        int parentId = DPUtil.parseInt(param.get("parentId"));
        return DPUtil.formatRelation(data, "parentId", parentId, "id", "children");
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dictionaryDao, param, (Specification<Dictionary>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            int ancestorId = DPUtil.parseInt(param.get("ancestorId"));
            // 为空时为所有字典列表，非空时为单个字典的所有字典项
            predicates.add(cb.equal(root.get("ancestorId"), ancestorId));
            int parentId = DPUtil.parseInt(param.get("parentId"));
            if (!"".equals(DPUtil.parseString(param.get("parentId")))) {
                predicates.add(cb.equal(root.get("parentId"), parentId));
            }
            String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
            if (!DPUtil.empty(content)) {
                predicates.add(cb.equal(root.get("content"), content));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("pinyin")), "id", "name", "pinyin", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if(!DPUtil.empty(args.get("withParentInfo"))) {
            this.fillInfo(rows, "ancestorId", "parentId");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            int leaf = node.at("/leaf").asInt(0);
            node.put("leaf", 1 == leaf);
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(dictionaryDao, ids);
    }

}
