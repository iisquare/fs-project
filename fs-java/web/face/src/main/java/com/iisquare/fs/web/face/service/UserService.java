package com.iisquare.fs.web.face.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.face.dao.GroupDao;
import com.iisquare.fs.web.face.dao.RelationDao;
import com.iisquare.fs.web.face.dao.UserDao;
import com.iisquare.fs.web.face.entity.Group;
import com.iisquare.fs.web.face.entity.Relation;
import com.iisquare.fs.web.face.entity.User;
import com.iisquare.fs.web.face.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class UserService extends ServiceBase {

    @Autowired
    private UserDao userDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private GroupDao groupDao;

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
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

    public User info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<User> info = userDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial)) return ApiUtil.result(1001, "人员标识不能为空", serial);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1002, "人员名称异常", name);
        User info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new User();
        }
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        info.setSerial(serial);
        info.setName(name);
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        if(uid > 0) {
            info.setUpdatedTime(time);
            info.setUpdatedUid(uid);
        }
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = userDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<User> data = userDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                int id = DPUtil.parseInt(param.get("id"));
                if(id > 0) predicates.add(cb.equal(root.get("id"), id));
                int status = DPUtil.parseInt(param.get("status"));
                if(!"".equals(DPUtil.parseString(param.get("status")))) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
                if(!DPUtil.empty(serial)) {
                    predicates.add(cb.equal(root.get("serial"), serial));
                }
                String createdTimeBegin = DPUtil.trim(DPUtil.parseString(param.get("createdTimeBegin")));
                if(!DPUtil.empty(createdTimeBegin)) {
                    predicates.add(cb.ge(root.get("createdTime"),
                            DPUtil.dateTime2millis(createdTimeBegin, configuration.getFormatDate())));
                }
                String createdTimeEnd = DPUtil.trim(DPUtil.parseString(param.get("createdTimeEnd")));
                if(!DPUtil.empty(createdTimeEnd)) {
                    predicates.add(cb.le(root.get("createdTime"),
                            DPUtil.dateTime2millis(createdTimeEnd, configuration.getFormatDate()) + 999));
                }
                String updatedTimeBegin = DPUtil.trim(DPUtil.parseString(param.get("updatedTimeBegin")));
                if(!DPUtil.empty(updatedTimeBegin)) {
                    predicates.add(cb.ge(root.get("updatedTime"),
                            DPUtil.dateTime2millis(updatedTimeBegin, configuration.getFormatDate())));
                }
                String updatedTimeEnd = DPUtil.trim(DPUtil.parseString(param.get("updatedTimeEnd")));
                if(!DPUtil.empty(updatedTimeEnd)) {
                    predicates.add(cb.le(root.get("updatedTime"),
                            DPUtil.dateTime2millis(updatedTimeEnd, configuration.getFormatDate()) + 999));
                }
                List<Integer> roleIds = (List<Integer>) param.get("groupIds");
                if(!DPUtil.empty(roleIds)) {
                    predicates.add(root.get("id").in(DPUtil.values(
                        relationDao.findAllByTypeAndBidIn("user_group", roleIds), Integer.class, "aid")));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withGroups")) && rows.size() > 0) {
            Map<Integer, User> rowsMap = DPUtil.list2map(rows, Integer.class, User.class, "id");
            Set<Integer> ids = rowsMap.keySet();
            List<Relation> relations = relationDao.findAllByTypeAndAidIn("user_group", ids);
            Set<Integer> groupIds = DPUtil.values(relations, Integer.class, "bid");
            Map<Integer, Group> groupMap = DPUtil.list2map(groupDao.findAllById(groupIds), Integer.class, Group.class, "id");
            for (Relation relation : relations) {
                User item = rowsMap.get(relation.getAid());
                if(null == item) continue;
                List<Group> groups = item.getGroups();
                if(null == groups) {
                    groups = new ArrayList<>();
                    item.setGroups(groups);
                }
                Group group = groupMap.get(relation.getBid());
                if(null == group) continue;
                groups.add(group);
            }
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        userDao.deleteInBatch(userDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<User> list = userDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (User item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        userDao.saveAll(list);
        return true;
    }

    public <T> List<T> fillInfo(List<T> list, String ...properties) {
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, User> data = DPUtil.list2map(userDao.findAllById(ids), Integer.class, "id");
        return DPUtil.fillValues(list, properties, "Info", data);
    }

}
