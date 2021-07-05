package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
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

    public ObjectNode identity(Integer id) {
        ObjectNode result = DPUtil.objectNode();
        User info = info(id);
        if (null == info) return result;
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("status", info.getStatus());
        ObjectNode roles = result.putObject("roles");
        Set<Integer> roleIds = DPUtil.values(relationDao.findAllByTypeAndAid("user_role", info.getId()), Integer.class, "bid");
        if (roleIds.size() > 0) {
            for (Role item : roleDao.findAllById(roleIds)) {
                if (1 != item.getStatus()) continue;
                ObjectNode role = roles.putObject(String.valueOf(item.getId()));
                role.put("id", item.getId());
                role.put("name", item.getName());
            }
        }
        return result;
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

    public ObjectNode search(Map<?, ?> param, Map<?, ?> args) {
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<User> data = userDao.findAll((Specification<User>) (root, query, cb) -> {
            SpecificationHelper helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").equal("serial").equal("createdIp");
            helper.betweenWithDate("createdTime").betweenWithDate("updatedTime").equal("loginedIp");
            helper.betweenWithDate("loginedTime").betweenWithDate("lockedTime");
            List<Integer> roleIds = helper.listInteger("roleIds");
            if(null != roleIds && roleIds.size() > 0) {
                helper.add(root.get("id").in(DPUtil.values(
                    relationDao.findAllByTypeAndBidIn("user_role", roleIds), Integer.class, "aid")));
            }
            return cb.and(helper.predicates());
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = this.filter(data.getContent());
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withRoles")) && rows.size() > 0) {
            Map<Integer, User> rowsMap = DPUtil.list2map(rows, Integer.class, User.class, "id");
            Set<Integer> ids = rowsMap.keySet();
            List<Relation> relations = relationDao.findAllByTypeAndAidIn("user_role", ids);
            Set<Integer> roleIds = DPUtil.values(relations, Integer.class, "bid");
            Map<Integer, Role> roleMap = DPUtil.list2map(roleDao.findAllById(roleIds), Integer.class, Role.class, "id");
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
        ObjectNode result = DPUtil.objectNode().put("page", page).put("pageSize", pageSize);
        result.put("total", data.getTotalElements()).putPOJO("rows", rows);
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

    public Map<Integer, User> infoByIds(List<Integer> ids) {
        if (null == ids || ids.size() < 1) return new HashMap<>();
        List<User> list = userDao.findAllById(ids);
        return DPUtil.list2map(list, Integer.class, "id");
    }

    public ObjectNode infos(List<Integer> ids) {
        ObjectNode result = DPUtil.objectNode();
        if (null == ids || ids.size() < 1) return result;
        List<User> list = userDao.findAllById(ids);
        for (User item : list) {
            if (1 != item.getStatus()) continue;
            ObjectNode node = result.putObject(String.valueOf(item.getId()));
            node.put("id", item.getId());
            node.put("name", item.getName());
        }
        return result;
    }

    public <T> List<T> fillInfo(List<T> list, String ...properties) {
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

    public ArrayNode fillInfo(ArrayNode array, String[] properties) {
        if(null == array || array.size() < 1 || properties.length < 1) return array;
        Set<Integer> ids = DPUtil.values(array, Integer.class, properties);
        if(ids.size() < 1) return array;
        Map<Integer, User> userInfos = DPUtil.list2map(userDao.findAllById(ids), Integer.class, "id");
        if(userInfos.size() < 1) return array;
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            for (String property : properties) {
                User user = userInfos.get(node.at("/" + property).asText(""));
                if(null == user) continue;
                node.put(property + "Name", user.getName());
            }
        }
        return array;
    }
}
