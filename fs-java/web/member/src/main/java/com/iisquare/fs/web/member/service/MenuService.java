package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.dao.RoleMenuDao;
import com.iisquare.fs.web.member.entity.Application;
import com.iisquare.fs.web.member.entity.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class MenuService extends JPAServiceBase {

    @Autowired
    MenuDao menuDao;
    @Autowired
    UserService userService;
    @Autowired
    ApplicationService applicationService;
    @Autowired
    RbacService rbacService;
    @Autowired
    RoleMenuDao roleMenuDao;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "关闭");
        return status;
    }

    public Menu info(Integer id) {
        return info(menuDao, id);
    }

    @Transactional
    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        int applicationId = DPUtil.parseInt(param.get("applicationId"));
        Application application = applicationService.info(applicationId); // 仅用于拼接全称，不校验所属应用
        int parentId = DPUtil.parseInt(param.get("parentId"));
        Menu parent = null;
        if(parentId < 0) {
            return ApiUtil.result(1005, "上级节点异常", name);
        } else if(parentId > 0) {
            parent = info(parentId);
            if(null == parent || !status().containsKey(parent.getStatus())) {
                return ApiUtil.result(1006, "上级节点不存在或已删除", name);
            }
            if(id > 0 && inside(id, parent)) {
                return ApiUtil.result(1008, "上级节点不能是自身或其子节点", name);
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
        String beforeFullName = null == info.getId() ? null : info.getFullName();
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
            info.setFullName((null == application ? "" : application.getName() + ":") + info.getName());
        } else {
            info.setFullName(parent.getFullName() + ":" + info.getName());
        }
        info = save(menuDao, info, rbacService.uid(request));
        if(!DPUtil.equals(beforeFullName, info.getFullName())) refreshFullName(info, new HashSet<>());
        rbacService.evictAllPermit(); // 菜单变更后同步清理资源缓存
        return ApiUtil.result(0, null, info);

    }

    /**
     * 判断上级节点是否位于指定节点及其子孙节点之中，避免形成环
     */
    private boolean inside(Integer id, Menu parent) {
        Set<Integer> visited = new HashSet<>();
        Menu node = parent;
        while (null != node) {
            if(DPUtil.equals(node.getId(), id)) return true;
            if(!visited.add(node.getId())) break; // 已有脏数据形成环，避免死循环
            int ancestorId = DPUtil.parseInt(node.getParentId());
            node = ancestorId > 0 ? info(ancestorId) : null;
        }
        return false;
    }

    /**
     * 名称或层级变化时同步子孙节点全称
     */
    private void refreshFullName(Menu node, Set<Integer> visited) {
        if(!visited.add(node.getId())) return;
        List<Menu> children = menuDao.findAll((Specification<Menu>) (root, query, cb) ->
                cb.equal(root.get("parentId"), node.getId()));
        if(children.isEmpty()) return;
        for (Menu child : children) {
            child.setFullName(node.getFullName() + ":" + child.getName());
        }
        menuDao.saveAll(children);
        for (Menu child : children) {
            refreshFullName(child, visited);
        }
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(menuDao, rows, properties);
    }

    public ArrayNode tree(Map<?, ?> param, Map<?, ?> args) {
        List<Menu> list = menuDao.findAll((Specification<Menu>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            // 未指定状态时不作过滤：权限分配需要覆盖该应用下的全部菜单节点
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
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
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

    @Transactional
    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.isEmpty()) return false;
        List<Integer> all = subtreeIds(ids); // 级联删除子孙节点，避免出现父节点悬空的孤儿节点
        roleMenuDao.deleteByMenuIdIn(all); // 菜单硬删除时清理授权关联
        rbacService.evictAllPermit(); // 菜单变更后同步清理资源缓存
        return remove(menuDao, all);
    }

    /**
     * 收集指定节点及其全部子孙节点标识
     */
    private List<Integer> subtreeIds(List<Integer> ids) {
        Set<Integer> result = new LinkedHashSet<>(ids);
        List<Integer> cursor = new ArrayList<>(ids);
        while (!cursor.isEmpty()) {
            List<Integer> parents = cursor;
            List<Menu> children = menuDao.findAll((Specification<Menu>) (root, query, cb) ->
                    root.get("parentId").in(parents));
            cursor = new ArrayList<>();
            for (Menu child : children) {
                if(result.add(child.getId())) cursor.add(child.getId());
            }
        }
        return new ArrayList<>(result);
    }

}
