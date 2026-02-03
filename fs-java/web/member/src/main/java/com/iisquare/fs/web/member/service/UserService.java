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
import com.iisquare.fs.web.member.core.RedisKey;
import com.iisquare.fs.web.member.dao.RelationDao;
import com.iisquare.fs.web.member.dao.RoleDao;
import com.iisquare.fs.web.member.dao.UserDao;
import com.iisquare.fs.web.member.entity.Relation;
import com.iisquare.fs.web.member.entity.Role;
import com.iisquare.fs.web.member.entity.User;
import com.iisquare.fs.web.member.mvc.Configuration;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
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
    @Autowired
    CaptchaService captchaService;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    EmailService emailService;

    public static final Integer LOGIN_TRY_TIMES = 6;

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

    public Map<String, Object> signup(Map<?, ?> param, HttpServletRequest request) {
        String action = DPUtil.parseString(param.get("action"));
        String captchaCode = DPUtil.parseString(param.get("captcha")); // 图形验证码
        String verifyCode = DPUtil.parseString(param.get("verify")); // 邮箱验证码
        if ("email".equals(action)) { // 发送邮箱验证码前，先校验图形验证码
            Map<String, Object> result = captchaService.verify(DPUtil.buildMap(
                    String.class, Object.class,
                    "uuid", DPUtil.parseString(param.get("uuid")),
                    "code", captchaCode
            ));
            if (ApiUtil.failed(result)) return result;
        } else {
            if (DPUtil.empty(captchaCode)) {
                return ApiUtil.result(1411, "请输入图形验证码", captchaCode);
            }
            if (DPUtil.empty(verifyCode)) {
                return ApiUtil.result(1412, "请输入邮箱验证码", verifyCode);
            }
        }
        String serial = DPUtil.parseString(param.get("serial"));
        if (!ValidateUtil.isUsername(serial)) {
            return ApiUtil.result(1001, "用户名不合法", serial);
        }
        String name = DPUtil.parseString(param.get("name"));
        if (!ValidateUtil.isNickname(name)) {
            return ApiUtil.result(1002, "昵称不合法", name);
        }
        String password = DPUtil.parseString(param.get("password"));
        if (DPUtil.empty(password)) {
            return ApiUtil.result(1003, "密码不能为空", password);
        }
        String confirm = DPUtil.parseString(param.get("confirm"));
        if (!password.equals(confirm)) {
            return ApiUtil.result(1004, "两次密码输入不一致", confirm);
        }
        String email = DPUtil.parseString(param.get("email"));
        if (!ValidateUtil.isEmail(email)) {
            return ApiUtil.result(1005, "邮箱地址不合法", email);
        }
        String redisKey = RedisKey.signup(email);
        ObjectNode verify = (ObjectNode) DPUtil.parseJSON(redis.opsForValue().get(redisKey), k -> DPUtil.objectNode());
        if ("email".equals(action)) { // 校验操作频率
            long time = verify.at("/time").asLong(0);
            if (time > 0 && Math.abs(System.currentTimeMillis() - time) < 120000) {
                return ApiUtil.result(12401, "当前验证码尚在有效期内，请稍后再试", time);
            }
        } else { // 校验图形验证码是否一致
            if (DPUtil.empty(captchaCode) || !captchaCode.equals(verify.at("/captcha").asText())) {
                return ApiUtil.result(13401, "图形验证码错误或已过期，请重新输入", captchaCode);
            }
        }
        List<User> users = userDao.findAll((Specification<User>) (root, query, cb) -> cb.or(
                cb.equal(root.get("serial"), serial),
                cb.equal(root.get("name"), name),
                cb.equal(root.get("email"), email)
        ));
        if (DPUtil.values(users, String.class, "serial").contains(serial)) {
            return ApiUtil.result(1011, "用户名已存在", serial);
        }
        if (DPUtil.values(users, String.class, "name").contains(name)) {
            return ApiUtil.result(1012, "昵称已存在", name);
        }
        if (DPUtil.values(users, String.class, "email").contains(email)) {
            return ApiUtil.result(1013, "邮箱地址已存在", email);
        }
        if ("email".equals(action)) { // 发送邮箱验证码
            String code = DPUtil.random(6);
            verify.put("time", System.currentTimeMillis());
            verify.put("email", email);
            verify.put("captcha", captchaCode);
            verify.put("code", code);
            redis.opsForValue().set(redisKey, verify.toString(), Duration.ofMinutes(5));
            return emailService.signup(email, code);
        }
        if (DPUtil.empty(verifyCode) || !verifyCode.equals(verify.at("/code").asText())) {
            return ApiUtil.result(13402, "邮箱验证码错误或已过期，请重新输入", param.get("verify"));
        }
        redis.delete(redisKey); // 清理验证码
        String salt = DPUtil.random(4);
        User.UserBuilder builder = User.builder().serial(serial).name(name).email(email);
        builder.createdIp(ServletUtil.getRemoteAddr(request));
        builder.password(password(password, salt)).salt(salt).status(1).description("自主注册");
        try {
            logout(request);
            User user = save(userDao, builder.build(), 0);
            return ApiUtil.result(0, "注册成功", DPUtil.firstNode(filter(DPUtil.toArrayNode(user))));
        } catch (Exception e) {
            return ApiUtil.result(1500, "注册失败，请稍后再试", e.getMessage());
        }
    }

    public Map<String, Object> forgot(Map<?, ?> param, HttpServletRequest request) {
        String action = DPUtil.parseString(param.get("action"));
        String captchaCode = DPUtil.parseString(param.get("captcha")); // 图形验证码
        String verifyCode = DPUtil.parseString(param.get("verify")); // 邮箱验证码
        if ("email".equals(action)) { // 发送邮箱验证码前，先校验图形验证码
            Map<String, Object> result = captchaService.verify(DPUtil.buildMap(
                    String.class, Object.class,
                    "uuid", DPUtil.parseString(param.get("uuid")),
                    "code", captchaCode
            ));
            if (ApiUtil.failed(result)) return result;
        } else {
            if (DPUtil.empty(captchaCode)) {
                return ApiUtil.result(1411, "请输入图形验证码", captchaCode);
            }
            if (DPUtil.empty(verifyCode)) {
                return ApiUtil.result(1412, "请输入邮箱验证码", verifyCode);
            }
        }
        String password = DPUtil.parseString(param.get("password"));
        if (DPUtil.empty(password)) {
            return ApiUtil.result(1003, "密码不能为空", password);
        }
        String confirm = DPUtil.parseString(param.get("confirm"));
        if (!password.equals(confirm)) {
            return ApiUtil.result(1004, "两次密码输入不一致", confirm);
        }
        String email = DPUtil.parseString(param.get("email"));
        if (!ValidateUtil.isEmail(email)) {
            return ApiUtil.result(1005, "邮箱地址不合法", email);
        }
        String redisKey = RedisKey.forgot(email);
        ObjectNode verify = (ObjectNode) DPUtil.parseJSON(redis.opsForValue().get(redisKey), k -> DPUtil.objectNode());
        if ("email".equals(action)) { // 校验操作频率
            long time = verify.at("/time").asLong(0);
            if (time > 0 && Math.abs(System.currentTimeMillis() - time) < 120000) {
                return ApiUtil.result(12401, "当前验证码尚在有效期内，请稍后再试", time);
            }
        } else { // 校验图形验证码是否一致
            if (DPUtil.empty(captchaCode) || !captchaCode.equals(verify.at("/captcha").asText())) {
                return ApiUtil.result(13401, "图形验证码错误或已过期，请重新输入", captchaCode);
            }
        }
        User user = userDao.findOne((Specification<User>) (root, query, cb) -> cb.and(
                cb.equal(root.get("email"), email),
                cb.equal(root.get("status"), 1)
        )).orElse(null);
        if (null == user) {
            return ApiUtil.result(1404, "邮箱地址不存在或用户状态异常", email);
        }
        if ("email".equals(action)) { // 发送邮箱验证码
            String code = DPUtil.random(6);
            verify.put("time", System.currentTimeMillis());
            verify.put("email", email);
            verify.put("captcha", captchaCode);
            verify.put("code", code);
            redis.opsForValue().set(redisKey, verify.toString(), Duration.ofMinutes(5));
            return emailService.forgot(email, code);
        }
        if (DPUtil.empty(verifyCode) || !verifyCode.equals(verify.at("/code").asText())) {
            return ApiUtil.result(13402, "邮箱验证码错误或已过期，请重新输入", param.get("verify"));
        }
        redis.delete(redisKey); // 清理验证码
        String salt = DPUtil.random(4);
        password = password(password, salt);
        user.setPassword(password);
        user.setSalt(salt);
        try {
            logout(request);
            user = userDao.save(user);
            return ApiUtil.result(0, "密码重置成功", DPUtil.firstNode(filter(DPUtil.toArrayNode(user))));
        } catch (Exception e) {
            return ApiUtil.result(1500, "密码重置失败，请稍后再试", e.getMessage());
        }
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
        if (!roleIds.isEmpty()) {
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
        Map<String, Object> session;
        String serial = DPUtil.parseString(param.get("serial"));
        String module = DPUtil.parseString(param.get("module"));
        if(DPUtil.empty(serial)) {
            session = rbacService.currentInfo(request, null);
            info = info(DPUtil.parseInt(session.get("uid")));
            if (null != info && (1 != info.getStatus() || info.getLockedTime() > System.currentTimeMillis())) {
                info = null;
                logout(request);
            }
        } else {
            Map<String, Object> result = captchaService.verify(DPUtil.buildMap(
                    String.class, Object.class,
                    "uuid", DPUtil.parseString(param.get("uuid")),
                    "code", DPUtil.parseString(param.get("captcha"))
            ));
            if (ApiUtil.failed(result)) return result;
            info = userDao.findOne((Specification<User>) (root, query, cb) -> {
                return cb.equal(root.get("serial"), serial);
            }).orElse(null);
            if(null == info || 0 != info.getDeletedTime()) {
                return ApiUtil.result(1001, "账号不存在或密码错误", null);
            }
            String redisKey = RedisKey.login(info.getId());
            if (DPUtil.parseInt(redis.opsForValue().get(redisKey)) > LOGIN_TRY_TIMES) {
                return ApiUtil.result(1401, "登录失败次数过多，请稍后再试", info.getId());
            }
            if(!info.getPassword().equals(password(DPUtil.parseString(param.get("password")), info.getSalt()))) {
                long increment = DPUtil.parseLong(redis.opsForValue().increment(redisKey, 1));
                if (1 == increment) {
                    redis.expire(redisKey, Duration.ofMinutes(30));
                }
                return ApiUtil.result(1002, String.format("登录失败，剩余%d次机会", LOGIN_TRY_TIMES - increment), null);
            }
            if(1 != info.getStatus() || info.getLockedTime() > System.currentTimeMillis()) {
                return ApiUtil.result(1003, "账号已锁定，请联系管理人员", null);
            }
            info.setLoginTime(System.currentTimeMillis());
            info.setLoginIp(ServletUtil.getRemoteAddr(request));
            userDao.save(info);
            // Session中仅存储标识信息，详情数据需单独存取，避免缓存不同步
            rbacService.currentInfo(request, DPUtil.buildMap("uid", info.getId()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("info", info(request, info));
        if ("admin".equals(module)) { // 非管理后台不加载权限和菜单配置，但仍具备单独访问授权资源的权限
            if(null != info && !rbacService.hasPermit(request, module, null, null)) {
                logout(request);
                return ApiUtil.result(403, null, null);
            }
            result.put("menu", rbacService.menu(request));
            result.put("resource", rbacService.resource(request));
        }
        return ApiUtil.result(0, null, result);
    }

    public ObjectNode info(HttpServletRequest request, User info) {
        ObjectNode result = DPUtil.objectNode();
        if (null == info) return result;
        result.put("id", info.getId());
        result.put("serial", info.getSerial());
        result.put("name", info.getName());
        result.put("email", info.getEmail());
        result.put("phone", info.getPhone());
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
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial) || !ValidateUtil.isUsername(serial)) {
            return ApiUtil.result(1001, "账号格式异常", serial);
        }
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name) || !ValidateUtil.isNickname(name)) {
            return ApiUtil.result(1002, "昵称格式异常", name);
        }
        String email = DPUtil.trim(DPUtil.parseString(param.get("email")));
        if (!DPUtil.empty(email) && !ValidateUtil.isEmail(email)) {
            return ApiUtil.result(1003, "邮箱地址异常", name);
        }
        String phone = DPUtil.trim(DPUtil.parseString(param.get("phone")));
        if (!DPUtil.empty(phone) && !ValidateUtil.isMobilePhone(phone)) {
            return ApiUtil.result(1004, "手机号码异常", phone);
        }
        List<User> list = userDao.findAll((Specification<User>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("serial"), serial));
            predicates.add(cb.equal(root.get("name"), name));
            if (!DPUtil.empty(email)) {
                predicates.add(cb.equal(root.get("email"), email));
            }
            if (!DPUtil.empty(phone)) {
                predicates.add(cb.equal(root.get("phone"), phone));
            }
            return cb.and(cb.or(predicates.toArray(new Predicate[0])), cb.notEqual(root.get("id"), id));
        });
        if (DPUtil.list2map(list, String.class, "serial").containsKey(serial)) {
            return ApiUtil.result(2001, "账号已存在", name);
        }
        if (DPUtil.list2map(list, String.class, "name").containsKey(name)) {
            return ApiUtil.result(2002, "昵称已存在", name);
        }
        if (!DPUtil.empty(email) && DPUtil.list2map(list, String.class, "email").containsKey(email)) {
            return ApiUtil.result(2003, "邮箱地址已存在", email);
        }
        if (!DPUtil.empty(phone) && DPUtil.list2map(list, String.class, "phone").containsKey(phone)) {
            return ApiUtil.result(2004, "手机号码已存在", phone);
        }
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
            info.setSerial(serial); // 账号不允许修改
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
            return ApiUtil.result(1012, "状态异常", status);
        }
        String description = DPUtil.parseString(param.get("description"));
        info.setName(name);
        info.setEmail(email);
        info.setPhone(phone);
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
            item.retain("id", "serial", "name");
        }
        return json;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(userDao, param, (Specification<User>) (root, query, cb) -> {
            SpecificationHelper<User> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.likeExp("serial").likeExp("name").likeExp("email").likeExp("phone");
            helper.equalWithIntNotEmpty("status").equal("createdIp").equal("loginIp");
            helper.betweenWithDate("createdTime").betweenWithDate("updatedTime");
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
