package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.member.dao.RelationDao;
import com.iisquare.fs.web.member.dao.RoleDao;
import com.iisquare.fs.web.member.dao.UserDao;
import com.iisquare.fs.web.member.entity.Relation;
import com.iisquare.fs.web.member.entity.Role;
import com.iisquare.fs.web.member.entity.User;
import com.iisquare.fs.web.member.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class UserService extends JPAServiceBase {

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
    private RelationService relationService;
    @Autowired
    private SettingService settingService;

    public User infoBySerial(String serial) {
        return userDao.findFirstBySerial(serial);
    }

    public boolean existsByName(String name, Integer ...ids) {
        return userDao.existsByNameEqualsAndIdNotIn(name, Arrays.asList(ids));
    }

    public boolean existsBySerial(String serial, Integer ...ids) {
        return userDao.existsBySerialEqualsAndIdNotIn(serial, Arrays.asList(ids));
    }

    public String password(String password, String salt) {
        return CodeUtil.md5(CodeUtil.md5(password) + salt);
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public User info(Integer id) {
        return info(userDao, id);
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
        User info;
        Map<String, Object> session = null;
        String module = DPUtil.parseString(param.get("module"));
        if(param.containsKey("serial")) {
            info = infoBySerial(DPUtil.parseString(param.get("serial")));
            if(null == info || 0 != info.getDeletedTime()) {
                return ApiUtil.result(1001, "账号不存在", null);
            }
            if(!info.getPassword().equals(password(DPUtil.parseString(param.get("password")), info.getSalt()))) {
                return ApiUtil.result(1002, "密码错误", null);
            }
            if(1 != info.getStatus() || info.getLockedTime() > System.currentTimeMillis()) {
                return ApiUtil.result(1003, "账号已锁定，请联系管理人员", null);
            }
            info.setLoginTime(System.currentTimeMillis());
            info.setLoginIp(ServletUtil.getRemoteAddr(request));
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("info", info(request, info));
        result.put("menu", rbacService.menu(request));
        result.put("resource", rbacService.resource(request));
        return ApiUtil.result(0, null, result);
    }

    public ObjectNode info(HttpServletRequest request, User info) {
        ObjectNode result = DPUtil.objectNode();
        if (null == info) return result;
        result.put("id", info.getId());
        result.put("serial", info.getSerial());
        result.put("name", info.getName());
        result.put("description", info.getDescription());
        result.put("createdIp", info.getCreatedIp());
        result.put("createdTime", info.getCreatedTime());
        result.put("loginIp", info.getLoginIp());
        result.put("loginTime", info.getLoginTime());
        result.put("token", request.getRequestedSessionId());
        return result;
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
        if(existsByName(name, id)) return ApiUtil.result(2001, "名称已存在", name);
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial)) return ApiUtil.result(1002, "账号不能为空", serial);
        if(existsByName(serial, id)) return ApiUtil.result(2001, "账号已存在", serial);
        String password = DPUtil.trim(DPUtil.parseString(param.get("password")));
        User info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
            if(0 != info.getDeletedTime()) return ApiUtil.result(1404, "用户已删除，不允许修改", id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new User();
            info.setSerial(serial);
            if(DPUtil.empty(password)) { // 若未设置密码，采用系统配置的默认密码
                password = settingService.get("member", "defaultPassword");
            }
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
        if(!status().containsKey(status)) {
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
        info = save(userDao, info, rbacService.uid(request));
        // 用户角色，新增时需要先保存用户
        List<Integer> roleIds = DPUtil.parseIntList(param.get("roleIds"));
        Set<Integer> permitted = relationService.relationIds("user_role", info.getId(), null);
        if (!relationService.same(roleIds, permitted)) {
            if(!rbacService.hasPermit(request, "role")) {
                return ApiUtil.result(0, "用户保存成功，无角色操作权限", info);
            }
            relationService.relationIds("user_role", info.getId(), new HashSet<>(roleIds));
        }
        return ApiUtil.result(0, null, info);
    }

    public JsonNode hide(JsonNode json) {
        for (JsonNode node : json) {
            ObjectNode item = (ObjectNode) node;
            item.remove(Arrays.asList("password", "salt"));
        }
        return json;
    }

    @Override
    public JsonNode filter(JsonNode json) {
        for (JsonNode node : json) {
            ObjectNode item = (ObjectNode) node;
            item.retain("id", "serial", "name", "id");
        }
        return json;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(userDao, param, (Specification<User>) (root, query, cb) -> {
            SpecificationHelper<User> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").equal("serial").equal("createdIp");
            helper.betweenWithDate("createdTime").betweenWithDate("updatedTime").equal("loginIp");
            helper.betweenWithDate("loginTime").betweenWithDate("lockedTime").betweenWithDate("deletedTime");
            List<Integer> roleIds = DPUtil.parseIntList(param.get("roleIds"));
            if(!roleIds.isEmpty()) {
                helper.add(root.get("id").in(DPUtil.values(
                        relationDao.findAllByTypeAndBidIn("user_role", roleIds), Integer.class, "aid")));
            }
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = hide(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            fillInfo(rows, "createdUid", "updatedUid", "deletedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if(!DPUtil.empty(args.get("withRoles")) && rows.size() > 0) {
            ObjectNode rowsMap = DPUtil.json2object(rows, "id");
            Set<Integer> ids = DPUtil.values(rowsMap, Integer.class, "id");
            List<Relation> relations = relationDao.findAllByTypeAndAidIn("user_role", ids);
            Set<Integer> roleIds = DPUtil.values(relations, Integer.class, "bid");
            Map<Integer, Role> roleMap = DPUtil.list2map(roleDao.findAllById(roleIds), Integer.class, Role.class, "id");
            for (Relation relation : relations) {
                ObjectNode item = (ObjectNode) rowsMap.at("/" + relation.getAid());
                if(null == item) continue;
                ArrayNode roles = item.has("roles") ? (ArrayNode) item.at("/roles") : item.putArray("roles");
                Role role = roleMap.get(relation.getBid());
                if(null == role) continue;
                roles.add(DPUtil.toJSON(role));
            }
        }
        return result;
    }

    public boolean delete(List<Integer> ids, HttpServletRequest request) {
        return delete(userDao, ids, rbacService.uid(request));
    }

    public ObjectNode infoByIds(List<Integer> ids) {
        return (ObjectNode) filter(infoByIds(userDao, ids));
    }

    public ObjectNode infos(List<Integer> ids) {
        ObjectNode nodes = infoByIds(userDao, ids);
        List<String> keys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = nodes.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue().at("/status").asInt() != 1) {
                keys.add(entry.getKey());
            }
        }
        nodes.remove(keys);
        return (ObjectNode) filter(nodes);
    }

    public JsonNode fillInfo(JsonNode json, String... properties) {
        return fillInfo("Uid", "UserInfo", json, properties);
    }

    public JsonNode fillInfo(String fromSuffix, String toSuffix, JsonNode json, String... properties) {
        return fillInfo(userDao, Integer.class, "id", fromSuffix, toSuffix, json, properties);
    }
}
