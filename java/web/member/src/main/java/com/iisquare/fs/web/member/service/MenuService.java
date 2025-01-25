package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.entity.Application;
import com.iisquare.fs.web.member.entity.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class MenuService extends JPAServiceBase {

    @Autowired
    private MenuDao menuDao;
    @Autowired
    private UserService userService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private RbacService rbacService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "关闭");
        return status;
    }

    public Menu info(Integer id) {
        return info(menuDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        int applicationId = DPUtil.parseInt(param.get("applicationId"));
        Application application = applicationService.info(applicationId);
        if(null == application) {
            return ApiUtil.result(1004, "所属应用不存在或已删除", name);
        }
        int parentId = DPUtil.parseInt(param.get("parentId"));
        Menu parent = null;
        if(parentId < 0) {
            return ApiUtil.result(1005, "上级节点异常", name);
        } else if(parentId > 0) {
            parent = info(parentId);
            if(null == parent || !status().containsKey(parent.getStatus())) {
                return ApiUtil.result(1006, "上级节点不存在或已删除", name);
            }
        }
        Menu info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Menu();
        }
        info.setName(name);
        info.setApplicationId(applicationId);
        info.setParentId(parentId);
        info.setIcon(DPUtil.trim(DPUtil.parseString(param.get("icon"))));
        info.setUrl(DPUtil.trim(DPUtil.parseString(param.get("url"))));
        info.setTarget(DPUtil.trim(DPUtil.parseString(param.get("target"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        if (null == parent) {
            info.setFullName(application.getName() + ":" + info.getName());
        } else {
            info.setFullName(parent.getFullName() + ":" + info.getName());
        }
        info = save(menuDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);

    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(menuDao, json, properties);
    }

    public ArrayNode tree(Map<?, ?> param, Map<?, ?> args) {
        List<Menu> list = menuDao.findAll((Specification<Menu>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            int applicationId = DPUtil.parseInt(param.get("applicationId"));
            if(!"".equals(DPUtil.parseString(param.get("applicationId")))) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("id")));
        ArrayNode data = DPUtil.toJSON(list, ArrayNode.class);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(data, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(data, status());
        }
        return DPUtil.formatRelation(data, "parentId", 0, "id", "children");
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(menuDao, param, (Specification<Menu>) (root, query, cb) -> {
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
            int applicationId = DPUtil.parseInt(param.get("applicationId"));
            if (!"".equals(DPUtil.parseString(param.get("applicationId")))) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            }
            int parentId = DPUtil.parseInt(param.get("parentId"));
            if (!"".equals(DPUtil.parseString(param.get("parentId")))) {
                predicates.add(cb.equal(root.get("parentId"), parentId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if(!DPUtil.empty(args.get("withApplicationInfo"))) {
            applicationService.fillInfo(rows, "applicationId");
        }
        if(!DPUtil.empty(args.get("withParentInfo"))) {
            this.fillInfo(rows, "parentId");
        }
        return result;
    }

    public boolean remove(List<Integer> ids) {
        return remove(menuDao, ids);
    }

}
