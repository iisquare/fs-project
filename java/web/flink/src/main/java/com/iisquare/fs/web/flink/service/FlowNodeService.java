package com.iisquare.fs.web.flink.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.flink.dao.FlowNodeDao;
import com.iisquare.fs.web.flink.entity.FlowNode;
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
import java.util.*;

@Service
public class FlowNodeService extends ServiceBase {

    @Autowired
    private FlowNodeDao flowNodeDao;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "关闭");
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

    public FlowNode info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<FlowNode> info = flowNodeDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public FlowNode save(FlowNode info, int uid) {
        long time = System.currentTimeMillis();
        FlowNode parent = info(info.getParentId());
        if (null == parent) {
            info.setFullName(info.getName());
        } else {
            info.setFullName(parent.getFullName() + ":" + info.getName());
        }
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return flowNodeDao.save(info);
    }

    public List<?> fillInfo(List<?> list, String ...properties) {
        if(null == list || list.size() < 1 || properties.length < 1) return list;
        Set<Integer> ids = ServiceUtil.getPropertyValues(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, FlowNode> map = ServiceUtil.indexObjectList(flowNodeDao.findAllById(ids), Integer.class, FlowNode.class, "id");
        if(map.size() < 1) return list;
        for (Object item : list) {
            for (String property : properties) {
                FlowNode info = map.get(ReflectUtil.getPropertyValue(item, property));
                if(null == info) continue;
                ReflectUtil.setPropertyValue(item, property + "Name", null, new Object[]{info.getName()});
            }
        }
        return list;
    }

    public List<FlowNode> tree(Map<?, ?> param, Map<?, ?> args) {
        List<FlowNode> data = flowNodeDao.findAll((Specification) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), -1));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        }, Sort.by(Sort.Order.desc("sort")));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(data, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            ServiceUtil.fillProperties(data, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        return ServiceUtil.formatRelation(data, FlowNode.class, "parentId", 0, "id", "children");
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<?> data = flowNodeDao.findAll(new Specification() {
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
                int parentId = DPUtil.parseInt(param.get("parentId"));
                if(!"".equals(DPUtil.parseString(param.get("parentId")))) {
                    predicates.add(cb.equal(root.get("parentId"), parentId));
                }
                String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
                if(!DPUtil.empty(type)) {
                    predicates.add(cb.equal(root.get("type"), type));
                }
                String plugin = DPUtil.trim(DPUtil.parseString(param.get("plugin")));
                if(!DPUtil.empty(plugin)) {
                    predicates.add(cb.equal(root.get("plugin"), plugin));
                }
                String classname = DPUtil.trim(DPUtil.parseString(param.get("classname")));
                if(!DPUtil.empty(classname)) {
                    predicates.add(cb.like(root.get("classname"), "%" + classname + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            ServiceUtil.fillProperties(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withParentInfo"))) {
            this.fillInfo(rows, "parentId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        flowNodeDao.deleteInBatch(flowNodeDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<FlowNode> list = flowNodeDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (FlowNode item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        flowNodeDao.saveAll(list);
        return true;
    }
}
