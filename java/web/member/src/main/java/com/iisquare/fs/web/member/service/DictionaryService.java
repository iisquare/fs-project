package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
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
        status.put(2, "关闭");
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
        int parentId = DPUtil.parseInt(param.get("parentId"));
        if(parentId < 0) {
            return ApiUtil.result(1003, "上级节点异常", name);
        } else if(parentId > 0) {
            Dictionary parent = info(parentId);
            if(null == parent || !status().containsKey(parent.getStatus())) {
                return ApiUtil.result(1004, "上级节点不存在或已删除", name);
            }
        }
        Dictionary info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Dictionary();
        }
        info.setName(name);
        info.setParentId(parentId);
        info.setContent(DPUtil.trim(DPUtil.parseString(param.get("content"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        Dictionary parent = info(info.getParentId());
        if (null == parent) {
            info.setFullName(info.getName());
        } else {
            info.setFullName(parent.getFullName() + ":" + info.getName());
        }
        info = save(dictionaryDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);

    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(dictionaryDao, json, properties);
    }

    public ObjectNode available(boolean withChildren) {
        ArrayNode list = this.tree(DPUtil.buildMap("status", 1), DPUtil.buildMap());
        return this.available(list, withChildren);
    }

    public ObjectNode findAvailable(ObjectNode available, String path, boolean withChildren) {
        String[] paths = DPUtil.explode("\\.", path);
        for (String key : paths) {
            JsonNode node = available.at("/" + key + "/children");
            if (!node.isObject()) return DPUtil.objectNode();
            available = (ObjectNode) node;
        }
        if (withChildren) return available;
        ObjectNode result = DPUtil.objectNode();
        Iterator<Map.Entry<String, JsonNode>> iterator = available.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            ObjectNode node = result.putObject(entry.getKey());
            JsonNode item = entry.getValue();
            node.put("label", item.get("label").asText());
            node.put("value", item.get("value").asText());
        }
        return result;
    }

    public ObjectNode available(ArrayNode list, boolean withChildren) {
        ObjectNode result = DPUtil.objectNode();
        if (null == list) return result;
        for (JsonNode dictionary : list) {
            String name = dictionary.at("/name").asText();
            String content = dictionary.at("/content").asText();
            ObjectNode node = result.putObject(content);
            node.put("label", name);
            node.put("value", content);
            if (!withChildren) continue;
            node.replace("children", available((ArrayNode) dictionary.at("/children"), withChildren));
        }
        return result;
    }

    public ArrayNode tree(Map<?, ?> param, Map<?, ?> args) {
        List<Dictionary> list = dictionaryDao.findAll((Specification<Dictionary>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort")));
        ArrayNode data = DPUtil.toJSON(list, ArrayNode.class);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(data, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(data, new String[]{"status"}, new String[]{"statusText"}, DPUtil.toJSON(status()));
        }
        return DPUtil.formatRelation(data, "parentId", 0, "id", "children");
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
            String fullName = DPUtil.trim(DPUtil.parseString(param.get("fullName")));
            if (!DPUtil.empty(fullName)) {
                predicates.add(cb.like(root.get("fullName"), "%" + fullName + "%"));
            }
            int parentId = DPUtil.parseInt(param.get("parentId"));
            if (!"".equals(DPUtil.parseString(param.get("parentId")))) {
                predicates.add(cb.equal(root.get("parentId"), parentId));
            }
            String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
            if (!DPUtil.empty(content)) {
                predicates.add(cb.equal(root.get("content"), content));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, "status", "statusText", status());
        }
        if(!DPUtil.empty(args.get("withParentInfo"))) {
            this.fillInfo(rows, "parentId");
        }
        return result;
    }

    public boolean remove(List<Integer> ids) {
        return remove(dictionaryDao, ids);
    }

}
