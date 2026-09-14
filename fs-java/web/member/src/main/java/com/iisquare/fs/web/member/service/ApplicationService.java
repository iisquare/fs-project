package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.ApplicationDao;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.dao.ResourceDao;
import com.iisquare.fs.web.member.dao.RoleApplicationDao;
import com.iisquare.fs.web.member.dao.RoleMenuDao;
import com.iisquare.fs.web.member.dao.RoleResourceDao;
import com.iisquare.fs.web.member.entity.Application;
import com.iisquare.fs.web.member.entity.Menu;
import com.iisquare.fs.web.member.entity.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class ApplicationService extends JPAServiceBase {

    @Autowired
    ApplicationDao applicationDao;
    @Autowired
    UserService userService;
    @Autowired
    RbacService rbacService;
    @Autowired
    MenuDao menuDao;
    @Autowired
    ResourceDao resourceDao;
    @Autowired
    RoleApplicationDao roleApplicationDao;
    @Autowired
    RoleMenuDao roleMenuDao;
    @Autowired
    RoleResourceDao roleResourceDao;

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
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public Application info(Integer id) {
        return info(applicationDao, id);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(applicationDao, rows, properties);
    }

    @Transactional
    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial)) return ApiUtil.result(1001, "标识异常", serial);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1002, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Application info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Application();
        }
        int count = applicationDao.exist(serial, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1003, "标识已存在", serial);
        }
        info.setSerial(serial);
        info.setName(name);
        info.setIcon(DPUtil.trim(DPUtil.parseString(param.get("icon"))));
        info.setUrl(DPUtil.trim(DPUtil.parseString(param.get("url"))));
        info.setTarget(DPUtil.trim(DPUtil.parseString(param.get("target"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(applicationDao, info, rbacService.uid(request));
        rbacService.evictAllPermit(); // 应用标识、状态变化会影响所有角色的资源缓存
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(applicationDao, param, (Specification<Application>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Set<Integer> ids = new TreeSet<>(DPUtil.parseIntList(param.get("id"))); // 支持下拉选择器批量回显
            ids.removeIf(item -> item < 1);
            if (!ids.isEmpty()) predicates.add(root.get("id").in(ids));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
            if (!DPUtil.empty(serial)) {
                predicates.add(cb.like(root.get("serial"), "%" + serial + "%"));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
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
        return result;
    }

    @Transactional
    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.isEmpty()) return false;
        roleApplicationDao.deleteByApplicationIdIn(ids); // 应用硬删除时清理授权关联
        List<Menu> menus = menuDao.findAll((Specification<Menu>) (root, query, cb) -> root.get("applicationId").in(ids));
        if(!menus.isEmpty()) roleMenuDao.deleteByMenuIdIn(DPUtil.values(menus, Integer.class, "id"));
        List<Resource> resources = resourceDao.findAll(
                (Specification<Resource>) (root, query, cb) -> root.get("applicationId").in(ids));
        if(!resources.isEmpty()) roleResourceDao.deleteByResourceIdIn(DPUtil.values(resources, Integer.class, "id"));
        menuDao.deleteAllByIdInBatch(DPUtil.values(menus, Integer.class, "id"));
        resourceDao.deleteAllByIdInBatch(DPUtil.values(resources, Integer.class, "id"));
        applicationDao.deleteInBatch(applicationDao.findAllById(ids));
        rbacService.evictAllPermit();
        return true;
    }

}
