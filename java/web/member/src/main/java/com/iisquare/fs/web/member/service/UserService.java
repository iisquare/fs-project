package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServletUtil;
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

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class UserService extends ServiceBase {

    @Autowired
    private UserDao userDao;
    @Autowired
    private Configuration configuration;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private SettingService settingService;

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

    public Map<String, Object> logout(HttpServletRequest request) {
        rbacService.currentInfo(request, DPUtil.buildMap("uid", 0));
        return ApiUtil.result(0, null, null);
    }

    public Map<String, Object> login(Map<?, ?> param, HttpServletRequest request) {
        User info = null;
        Map<String, Object> session = null;
        String module = DPUtil.parseString(param.get("module"));
        if(param.containsKey("serial")) {
            info = infoBySerial(DPUtil.parseString(param.get("serial")));
            if(null == info) return ApiUtil.result(1001, "账号不存在", null);
            if(!info.getPassword().equals(password(DPUtil.parseString(param.get("password")), info.getSalt()))) {
                return ApiUtil.result(1002, "密码错误", null);
            }
            if(1 != info.getStatus() || info.getLockedTime() > System.currentTimeMillis()) {
                return ApiUtil.result(1003, "账号已锁定，请联系管理人员", null);
            }
            info.setLoginedTime(System.currentTimeMillis());
            info.setLoginedIp(ServletUtil.getRemoteAddr(request));
            userDao.save(info);
            rbacService.currentInfo(request, DPUtil.buildMap("uid", info.getId()));
            if(!rbacService.hasPermit(request, module, null, null)) {
                logout(request);
                return ApiUtil.result(403, null, null);
            }
        } else {
            session = rbacService.currentInfo(request, null);
            info = info(DPUtil.parseInt(session.get("uid")));
        }
        if(null != info) {
            info.setPassword("******");
            info.setSalt("******");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("info", info);
        result.put("menu", rbacService.menu(request));
        result.put("resource", rbacService.resource(request));
        return ApiUtil.result(0, null, result);
    }
    
    public Map<String, Object> password(Map<?, ?> param, HttpServletRequest request) {
        String password = DPUtil.trim(DPUtil.parseString(param.get("password")));
        String passwordNew = DPUtil.trim(DPUtil.parseString(param.get("passwordNew")));
        String passwordOld = DPUtil.trim(DPUtil.parseString(param.get("passwordOld")));
        if(DPUtil.empty(passwordOld)) return ApiUtil.result(1001, "请输入原密码", null);
        if(DPUtil.empty(password)) return ApiUtil.result(1002, "请输入新密码", null);
        if(!password.equals(passwordNew)) return ApiUtil.result(1003, "两次密码输入不一致", null);
        User info = info(rbacService.uid(request));
        if(null == info) return ApiUtil.result(1004, "用户未登录或登录超时", null);
        if(!info.getPassword().equals(password(passwordOld, info.getSalt()))) {
            return ApiUtil.result(1005, "原密码错误", null);
        }
        String salt = DPUtil.random(4);
        password = password(password, salt);
        info.setPassword(password);
        info.setSalt(salt);
        userDao.save(info);
        logout(request); // 退出登录
        return ApiUtil.result(0, null, null);
    }
    
    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        if(existsByName("name", id)) return ApiUtil.result(2001, "名称已存在", name);
        String password = DPUtil.trim(DPUtil.parseString(param.get("password")));
        User info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new User();
            String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
            if(DPUtil.empty(serial)) return ApiUtil.result(1002, "账号不能为空", serial);
            if(existsBySerial(serial)) return ApiUtil.result(2002, "账号已存在", serial);
            info.setSerial(serial);
            if(DPUtil.empty(password)) return ApiUtil.result(1003, "密码不能为空", password);
            info.setCreatedIp(ServletUtil.getRemoteAddr(request));
        }
        if(!DPUtil.empty(password)) {
            String salt = DPUtil.random(4);
            password = password(password, salt);
            info.setPassword(password);
            info.setSalt(salt);
        }
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) {
            return ApiUtil.result(1002, "状态异常", status);
        }
        String description = DPUtil.parseString(param.get("description"));
        info.setName(name);
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        if(param.containsKey("lockedTime")) {
            String lockedTime =  DPUtil.trim(DPUtil.parseString(param.get("lockedTime")));
            if(DPUtil.empty(lockedTime)) {
                info.setLockedTime(0L);
            } else {
                info.setLockedTime(DPUtil.dateTime2millis(lockedTime, configuration.getFormatDate()));
            }
        }
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
            fillInfo(rows, "createdUid", "updatedUid");
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
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, User> data = DPUtil.list2map(userDao.findAllById(ids), Integer.class, "id");
        return DPUtil.fillValues(list, properties, "Name", DPUtil.values(data, String.class, "name"));
    }

    public ArrayNode fillInfo(ArrayNode array, String[] properties) {
        Set<Integer> ids = DPUtil.values(array, Integer.class, properties);
        if(ids.size() < 1) return array;
        Map<Integer, User> data = DPUtil.list2map(userDao.findAllById(ids), Integer.class, "id");
        JsonNode reflect = DPUtil.toJSON(DPUtil.values(data, String.class, "name"));
        return DPUtil.fillValues(array, properties, DPUtil.suffix(properties, "Name"), reflect);
    }
}
