package com.iisquare.fs.web.member.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.member.dao.ResourceDao;
import com.iisquare.fs.web.member.entity.Resource;
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
public class ResourceService extends ServiceBase {

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private UserService userService;

    public List<Resource> tree(Map<?, ?> param, Map<?, ?> args) {
        List<Resource> data = resourceDao.findAll((Specification) (root, query, cb) -> {
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
            userService.fillInfo(data, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(data, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        return DPUtil.formatRelation(data, Resource.class, "parentId", 0, "id", "children");
    }

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

    public Resource info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Resource> info = resourceDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Resource save(Resource info, int uid) {
        long time = System.currentTimeMillis();
        Resource parent = info(info.getParentId());
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
        return resourceDao.save(info);
    }

    public List<?> fillInfo(List<?> list, String ...properties) {
        if(null == list || list.size() < 1 || properties.length < 1) return list;
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, Resource> map = DPUtil.list2map(resourceDao.findAllById(ids), Integer.class, Resource.class, "id");
        if(map.size() < 1) return list;
        for (Object item : list) {
            for (String property : properties) {
                Resource info = map.get(ReflectUtil.getPropertyValue(item, property));
                if(null == info) continue;
                ReflectUtil.setPropertyValue(item, property + "Name", null, new Object[]{info.getName()});
            }
        }
        return list;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<?> data = resourceDao.findAll(new Specification() {
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
                String fullName = DPUtil.trim(DPUtil.parseString(param.get("fullName")));
                if(!DPUtil.empty(fullName)) {
                    predicates.add(cb.like(root.get("fullName"), "%" + fullName + "%"));
                }
                int parentId = DPUtil.parseInt(param.get("parentId"));
                if(!"".equals(DPUtil.parseString(param.get("parentId")))) {
                    predicates.add(cb.equal(root.get("parentId"), parentId));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
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
        resourceDao.deleteInBatch(resourceDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Resource> list = resourceDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Resource item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        resourceDao.saveAll(list);
        return true;
    }

}
