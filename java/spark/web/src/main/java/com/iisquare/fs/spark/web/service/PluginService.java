package com.iisquare.fs.spark.web.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.spark.web.dao.PluginDao;
import com.iisquare.fs.spark.web.entity.Plugin;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.util.*;

@Service
public class PluginService extends ServiceBase {

    @Autowired
    private PluginDao pluginDao;
    @Autowired
    private DefaultRbacService rbacService;

    public boolean exists(String name) {
        return pluginDao.existsByNameAndStatusNotIn(name, Arrays.asList(-1));
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

    public Plugin info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Plugin> info = pluginDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Plugin> infoMap(Collection<String> names) {
        if(DPUtil.empty(names)) return new LinkedHashMap<>();
        List<Plugin> list = pluginDao.findAllByStatusAndNameIn(1, names);
        return DPUtil.list2map(list, String.class, Plugin.class, "name");
    }

    public Plugin save(Plugin info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return pluginDao.save(info);
    }

    public ArrayNode tree(File dir) {
        ArrayNode list = DPUtil.arrayNode();
        if(null == dir || !dir.isDirectory()) return list;
        for (File file : dir.listFiles()) {
            ObjectNode item = DPUtil.objectNode();
            item.put("name", file.getName());
            if(file.isDirectory()) {
                item.replace("children", tree(file));
            }
            list.add(item);
        }
        return list;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "name"));
        if (null == sort) sort = Sort.by(Sort.Order.asc("name"));
        Page<?> data = pluginDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                int id = DPUtil.parseInt(param.get("id"));
                if(id > 0) predicates.add(cb.equal(root.get("id"), id));
                int status = DPUtil.parseInt(param.get("status"));
                if(!"".equals(DPUtil.parseString(param.get("status")))) {
                    predicates.add(cb.equal(root.get("status"), status));
                } else {
                    predicates.add(cb.notEqual(root.get("status"), -1));
                }
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                String version = DPUtil.trim(DPUtil.parseString(param.get("version")));
                if(!DPUtil.empty(version)) {
                    predicates.add(cb.equal(root.get("version"), version));
                }
                String config = DPUtil.trim(DPUtil.parseString(param.get("config")));
                if(!DPUtil.empty(config)) {
                    predicates.add(cb.like(root.get("config"), "%" + config + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        pluginDao.deleteInBatch(pluginDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Plugin> list = pluginDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Plugin item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        pluginDao.saveAll(list);
        return true;
    }
}
