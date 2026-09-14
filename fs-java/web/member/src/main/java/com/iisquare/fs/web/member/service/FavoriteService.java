package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.FavoriteDao;
import com.iisquare.fs.web.member.entity.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class FavoriteService extends JPAServiceBase {

    @Autowired
    FavoriteDao favoriteDao;
    @Autowired
    UserService userService;
    @Autowired
    RbacService rbacService;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("name", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<?, ?> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("sql", "查询语句");
        return types;
    }

    public Favorite info(Integer id) {
        return info(favoriteDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(!types().containsKey(type)) return ApiUtil.result(1002, "类型异常", type);
        String content = DPUtil.parseString(param.get("content"));
        if(DPUtil.empty(content)) return ApiUtil.result(1003, "内容异常", content);
        int uid = rbacService.uid(request);
        Favorite info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
            if (info.getCreatedUid() != uid) return ApiUtil.result(1403, "请联系作者更新", uid);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Favorite();
        }
        info.setName(name);
        info.setType(type);
        info.setContent(content);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setSharable(DPUtil.parseBoolean(param.get("sharable")) ? 1 : 0);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(favoriteDao, info, uid);
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        ObjectNode result = search(favoriteDao, param, (Specification<Favorite>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if (!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            String share = DPUtil.parseString(param.get("share"));
            switch (share) {
                case "only": // 仅查看他人分享
                    predicates.add(cb.and(
                            cb.equal(root.get("sharable"), 1),
                            cb.notEqual(root.get("createdUid"), uid)
                    ));
                    break;
                case "without": // 仅查看自己的
                    predicates.add(cb.equal(root.get("createdUid"), uid));
                    break;
                default: // 查看全部，含自己创建和他人分享的
                    predicates.add(cb.or(
                            cb.equal(root.get("sharable"), 1),
                            cb.equal(root.get("createdUid"), uid)
                    ));
                    break;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("name"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.put("sharable", 1 == node.at("/sharable").asInt(0));
        }
        return rows;
    }

    public long remove(List<Integer> ids, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        return favoriteDao.delete((Specification<Favorite>) (root, query, cb) -> {
            return cb.and(
                    root.get("id").in(ids),
                    cb.equal(root.get("createdUid"), uid)
            );
        });
    }

}
