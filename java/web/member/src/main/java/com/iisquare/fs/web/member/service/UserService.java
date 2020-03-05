package com.iisquare.fs.web.member.service;

import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.member.dao.RelationDao;
import com.iisquare.fs.web.member.dao.RoleDao;
import com.iisquare.fs.web.member.dao.UserDao;
import com.iisquare.fs.web.member.entity.Relation;
import com.iisquare.fs.web.member.entity.Role;
import com.iisquare.fs.web.member.entity.User;
import com.iisquare.fs.web.member.mvc.Configuration;
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
public class UserService extends ServiceBase {

    @Autowired
    private UserDao userDao;
    @Autowired
    private UserService userService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private RoleDao roleDao;

    public User infoBySerial(String serial) {
        return userDao.findFirstBySerial(serial);
    }

    public boolean existsByName(String name, Integer ...ids) {
        return userDao.existsByNameEqualsAndIdNotIn(name, Arrays.asList(ids));
    }

    public boolean existsBySerial(String serial) {
        return userDao.existsBySerial(serial);
    }

    public String password(String password, String salt) {
        return CodeUtil.md5(CodeUtil.md5(password) + salt);
    }

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

    public User save(User info, int uid) {
        long time = System.currentTimeMillis();
        if(uid > 0) {
            info.setUpdatedTime(time);
            info.setUpdatedUid(uid);
        }
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return userDao.save(info);
    }

    public List<User> filter(List<User> list) {
        for (User item : list) {
            item.setPassword("");
            item.setSalt("");
        }
        return list;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<User> data = userDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
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
                String createdIp = DPUtil.trim(DPUtil.parseString(param.get("createdIp")));
                if(!DPUtil.empty(createdIp)) {
                    predicates.add(cb.equal(root.get("createdIp"), createdIp));
                }
                String createdTimeStart = DPUtil.trim(DPUtil.parseString(param.get("createdTimeStart")));
                if(!DPUtil.empty(createdTimeStart)) {
                    predicates.add(cb.ge(root.get("createdTime"), DPUtil.dateTimeToMillis(createdTimeStart, configuration.getDateFormat())));
                }
                String createdTimeEnd = DPUtil.trim(DPUtil.parseString(param.get("createdTimeEnd")));
                if(!DPUtil.empty(createdTimeEnd)) {
                    predicates.add(cb.le(root.get("createdTime"), DPUtil.dateTimeToMillis(createdTimeEnd, configuration.getDateFormat())));
                }
                String updatedTimeStart = DPUtil.trim(DPUtil.parseString(param.get("updatedTimeStart")));
                if(!DPUtil.empty(updatedTimeStart)) {
                    predicates.add(cb.ge(root.get("updatedTime"), DPUtil.dateTimeToMillis(updatedTimeStart, configuration.getDateFormat())));
                }
                String updatedTimeEnd = DPUtil.trim(DPUtil.parseString(param.get("updatedTimeEnd")));
                if(!DPUtil.empty(updatedTimeEnd)) {
                    predicates.add(cb.le(root.get("updatedTime"), DPUtil.dateTimeToMillis(updatedTimeEnd, configuration.getDateFormat())));
                }
                String loginedIp = DPUtil.trim(DPUtil.parseString(param.get("loginedIp")));
                if(!DPUtil.empty(loginedIp)) {
                    predicates.add(cb.equal(root.get("loginedIp"), loginedIp));
                }
                String loginedTimeStart = DPUtil.trim(DPUtil.parseString(param.get("loginedTimeStart")));
                if(!DPUtil.empty(loginedTimeStart)) {
                    predicates.add(cb.ge(root.get("loginedTime"), DPUtil.dateTimeToMillis(loginedTimeStart, configuration.getDateFormat())));
                }
                String loginedTimeEnd = DPUtil.trim(DPUtil.parseString(param.get("loginedTimeEnd")));
                if(!DPUtil.empty(loginedTimeEnd)) {
                    predicates.add(cb.le(root.get("loginedTime"), DPUtil.dateTimeToMillis(loginedTimeEnd, configuration.getDateFormat())));
                }
                String lockedTimeStart = DPUtil.trim(DPUtil.parseString(param.get("lockedTimeStart")));
                if(!DPUtil.empty(lockedTimeStart)) {
                    predicates.add(cb.ge(root.get("lockedTime"), DPUtil.dateTimeToMillis(lockedTimeStart, configuration.getDateFormat())));
                }
                String lockedTimeEnd = DPUtil.trim(DPUtil.parseString(param.get("lockedTimeEnd")));
                if(!DPUtil.empty(lockedTimeEnd)) {
                    predicates.add(cb.le(root.get("lockedTime"), DPUtil.dateTimeToMillis(lockedTimeEnd, configuration.getDateFormat())));
                }
                List<Integer> roleIds = (List<Integer>) param.get("roleIds");
                if(!DPUtil.empty(roleIds)) {
                    predicates.add(root.get("id").in(ServiceUtil.getPropertyValues(
                        relationDao.findAllByTypeAndBidIn("user_role", roleIds), Integer.class, "aid")));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<?> rows = this.filter(data.getContent());
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            ServiceUtil.fillProperties(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(config.get("withRoles")) && rows.size() > 0) {
            Map<Integer, User> rowsMap = ServiceUtil.indexObjectList(rows, Integer.class, User.class, "id");
            Set<Integer> ids = rowsMap.keySet();
            List<Relation> relations = relationDao.findAllByTypeAndAidIn("user_role", ids);
            Set<Integer> roleIds = ServiceUtil.getPropertyValues(relations, Integer.class, "bid");
            Map<Integer, Role> roleMap = ServiceUtil.indexObjectList(roleDao.findAllById(roleIds), Integer.class, Role.class, "id");
            for (Relation relation : relations) {
                User item = rowsMap.get(relation.getAid());
                if(null == item) continue;
                List<Role> roles = item.getRoles();
                if(null == roles) {
                    roles = new ArrayList<>();
                    item.setRoles(roles);
                }
                Role role = roleMap.get(relation.getBid());
                if(null == role) continue;
                roles.add(role);
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

    public List<?> fillInfo(List<?> list, String ...properties) {
        if(null == list || list.size() < 1 || properties.length < 1) return list;
        Set<Integer> ids = new HashSet<>();
        for (Object item : list) {
            for (String property : properties) {
                int id = DPUtil.parseInt(ReflectUtil.getPropertyValue(item, property));
                if(id < 1) continue;
                ids.add(DPUtil.parseInt(id));
            }
        }
        if(ids.size() < 1) return list;
        List<User> users = userDao.findAllById(ids);
        Map<Integer, User> map = new HashMap<>();
        for (User item : users) {
            map.put(item.getId(), item);
        }
        if(users.size() < 1) return list;
        for (Object item : list) {
            for (String property : properties) {
                User user = map.get(ReflectUtil.getPropertyValue(item, property));
                if(null == user) continue;
                ReflectUtil.setPropertyValue(item, property + "Name", new Class[]{String.class}, new Object[]{user.getName()});
            }
        }
        return list;
    }

}
