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
import com.iisquare.fs.web.member.dao.RoleDao;
import com.iisquare.fs.web.member.dao.UserDao;
import com.iisquare.fs.web.member.dao.UserRoleDao;
import com.iisquare.fs.web.member.entity.Role;
import com.iisquare.fs.web.member.entity.User;
import com.iisquare.fs.web.member.entity.UserRole;
import com.iisquare.fs.web.member.mvc.Configuration;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.*;

@Service
public class UserService extends JPAServiceBase {

    @Autowired
    UserDao userDao;
    @Autowired
    Configuration configuration;
    @Autowired
    RoleDao roleDao;
    @Autowired
    UserRoleDao userRoleDao;
    @Autowired
    RbacService rbacService;
    @Autowired
    SettingService settingService;
    @Autowired
    CaptchaService captchaService;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    MessageService messageService;

    public static final Integer LOGIN_TRY_TIMES = 6;
    public static final Integer VERIFY_TRY_TIMES = 5; // 邮箱验证码最大尝试次数
    public static final Integer PASSWORD_TRY_TIMES = 5; // 修改密码时原密码最大尝试次数

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
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
        if (DPUtil.values(users, String.class, "serial").contains(serial)
                || DPUtil.values(users, String.class, "name").contains(name)
                || DPUtil.values(users, String.class, "email").contains(email)) {
            return ApiUtil.result(1011, "注册信息已存在", null); // 统一文案，避免账号枚举
        }
        if ("email".equals(action)) { // 发送邮箱验证码
            String code = DPUtil.random(6);
            verify.put("time", System.currentTimeMillis());
            verify.put("email", email);
            verify.put("captcha", captchaCode);
            verify.put("code", code);
            redis.opsForValue().set(redisKey, verify.toString(), Duration.ofMinutes(5));
            return messageService.signup(email, code);
        }
        if (DPUtil.empty(verifyCode)) {
            return ApiUtil.result(13402, "邮箱验证码错误或已过期，请重新输入", param.get("verify"));
        }
        if (!verifyCode.equals(verify.at("/code").asText())) {
            Map<String, Object> retry = verifyRetry(redisKey, verify);
            if (null != retry) return retry;
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
            if ("email".equals(action)) {
                return ApiUtil.result(0, "验证码已发送，请注意查收", email); // 不发送也不提示邮箱是否存在，避免枚举
            }
            return ApiUtil.result(13402, "验证码错误或已过期，请重新输入", param.get("verify")); // 不提示邮箱是否存在
        }
        if ("email".equals(action)) { // 发送邮箱验证码
            String code = DPUtil.random(6);
            verify.put("time", System.currentTimeMillis());
            verify.put("email", email);
            verify.put("captcha", captchaCode);
            verify.put("code", code);
            redis.opsForValue().set(redisKey, verify.toString(), Duration.ofMinutes(5));
            return messageService.forgot(email, code);
        }
        if (DPUtil.empty(verifyCode)) {
            return ApiUtil.result(13402, "邮箱验证码错误或已过期，请重新输入", param.get("verify"));
        }
        if (!verifyCode.equals(verify.at("/code").asText())) {
            Map<String, Object> retry = verifyRetry(redisKey, verify);
            if (null != retry) return retry;
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
            rbacService.removeSessions(user.getId()); // 密码重置后使该用户所有会话失效
            return ApiUtil.result(0, "密码重置成功", DPUtil.firstNode(filter(DPUtil.toArrayNode(user))));
        } catch (Exception e) {
            return ApiUtil.result(1500, "密码重置失败，请稍后再试", e.getMessage());
        }
    }

    /**
     * 验证码校验失败时递增尝试次数，超过上限后作废验证码，需重新获取
     */
    private Map<String, Object> verifyRetry(String redisKey, ObjectNode verify) {
        int retry = verify.at("/retry").asInt(0) + 1;
        if (retry >= VERIFY_TRY_TIMES) {
            redis.delete(redisKey);
            return ApiUtil.result(13403, "验证码尝试次数过多，请重新获取", retry);
        }
        verify.put("retry", retry);
        redis.opsForValue().set(redisKey, verify.toString(), Duration.ofMinutes(5));
        return null;
    }

    /**
     * 用户身份信息，全部由用户与角色缓存组装，不直接访问数据库
     * 用户不存在、已删除或账号锁定期间返回空对象，视为不可用
     */
    public ObjectNode identity(Integer id) {
        ObjectNode permit = rbacService.userPermit(id);
        if (1 != permit.at("/status").asInt() || 0 != permit.at("/deletedTime").asLong()
                || permit.at("/lockedTime").asLong() > System.currentTimeMillis()) {
            return DPUtil.objectNode();
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("id", permit.at("/id").asInt());
        result.put("serial", permit.at("/serial").asText());
        result.put("name", permit.at("/name").asText());
        result.put("email", permit.at("/email").asText());
        result.put("phone", permit.at("/phone").asText());
        ObjectNode roles = result.putObject("roles");
        Set<Integer> roleIds = new TreeSet<>();
        for (JsonNode roleId : permit.at("/roles")) {
            roleIds.add(roleId.asInt());
        }
        for (ObjectNode role : rbacService.rolePermits(roleIds)) {
            if (1 != role.at("/status").asInt()) continue; // 角色不存在或未启用时不返回
            ObjectNode node = roles.putObject(String.valueOf(role.at("/id").asInt()));
            node.put("id", role.at("/id").asInt());
            node.put("name", role.at("/name").asText());
        }
        return result;
    }

    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.invalidate(); // Spring Session 会同步清理 Redis 中的会话及索引数据
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
            if (null != info && (1 != info.getStatus() || 0 != info.getDeletedTime() || info.getLockedTime() > System.currentTimeMillis())) {
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
            String redisKey = RedisKey.login(serial); // 按账号计数，账号不存在时同样累计，避免账号枚举
            if (DPUtil.parseLong(redis.opsForValue().get(redisKey)) >= LOGIN_TRY_TIMES) {
                return ApiUtil.result(1401, "登录失败次数过多，请稍后再试", null);
            }
            if(null == info || 0 != info.getDeletedTime()
                    || !info.getPassword().equals(password(DPUtil.parseString(param.get("password")), info.getSalt()))) {
                long increment = DPUtil.parseLong(redis.opsForValue().increment(redisKey, 1));
                if (1 == increment) {
                    redis.expire(redisKey, Duration.ofMinutes(30));
                }
                return ApiUtil.result(1001, "账号不存在或密码错误", null); // 统一文案，避免账号枚举
            }
            redis.delete(redisKey); // 登录成功，清理失败计数
            if(1 != info.getStatus() || info.getLockedTime() > System.currentTimeMillis()) {
                return ApiUtil.result(1003, "账号已锁定，请联系管理人员", null);
            }
            info.setLoginTime(System.currentTimeMillis());
            info.setLoginIp(ServletUtil.getRemoteAddr(request));
            userDao.save(info);
            // Session中仅存储标识信息，详情数据需单独存取，避免缓存不同步
            rbacService.currentInfo(request, DPUtil.buildMap("uid", info.getId()));
            request.changeSessionId(); // 登录成功后轮换会话标识，避免会话固定攻击
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
        result.put("token", request.getSession().getId());
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
        String redisKey = RedisKey.password(info.getId()); // 按用户计数，防止暴力猜解原密码
        if (DPUtil.parseLong(redis.opsForValue().get(redisKey)) >= PASSWORD_TRY_TIMES) {
            return ApiUtil.result(1401, "尝试次数过多，请稍后再试", null);
        }
        if(!info.getPassword().equals(password(passwordOld, info.getSalt()))) {
            long increment = DPUtil.parseLong(redis.opsForValue().increment(redisKey, 1));
            if (1 == increment) redis.expire(redisKey, Duration.ofMinutes(30));
            return ApiUtil.result(1005, "原密码错误", null);
        }
        redis.delete(redisKey); // 原密码校验通过，清理失败计数
        String salt = DPUtil.random(4);
        password = password(password, salt);
        info.setPassword(password);
        info.setSalt(salt);
        userDao.save(info);
        int count = rbacService.removeSessions(info.getId());// 使该用户所有会话失效
        request.getSession().invalidate();
        return ApiUtil.result(0, null, count);
    }
    
    @Transactional
    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial) || (DPUtil.parseInt(id) <= 0 && !ValidateUtil.isUsername(serial))) {
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
        // 授权角色维护在独立关联表中，无操作权限时保留原值
        Set<Integer> roleIds = new TreeSet<>(DPUtil.parseIntList(param.get("roleIds")));
        if (!roleIds.isEmpty() && !roleIds.equals(DPUtil.values(roleDao.findAllById(roleIds), Integer.class, "id"))) {
            return ApiUtil.result(1006, "授权角色不存在或已删除", null);
        }
        if(null != info.getId()) userDao.findByIdForUpdate(info.getId()); // 悲观锁，避免并发覆盖
        Set<Integer> permitted = new TreeSet<>(userRoleDao.findRoleIdsByUserId(info.getId()));
        boolean roleDenied = !roleIds.equals(permitted) && !rbacService.hasPermit(request, "role");
        info = save(userDao, info, rbacService.uid(request));
        if(!roleDenied) updateUserRole(info.getId(), permitted, roleIds, rbacService.uid(request));
        if(1 != DPUtil.parseInt(info.getStatus()) || DPUtil.parseLong(info.getLockedTime()) > System.currentTimeMillis()) {
            rbacService.removeSessions(info.getId()); // 禁用或锁定后立即失效在线会话
        }
        rbacService.evictUserPermit(info.getId()); // 用户角色或状态变更后同步清理该用户的资源缓存
        JsonNode node = DPUtil.firstNode(hide(DPUtil.toArrayNode(info))); // 不返回密码、密码盐等敏感字段
        if (roleDenied) return ApiUtil.result(0, "用户保存成功，无角色操作权限", node);
        return ApiUtil.result(0, null, node);
    }

    /**
     * 增量更新用户角色，仅写入新增与解除的部分，未改动的授权保持原样
     */
    private void updateUserRole(Integer userId, Set<Integer> current, Set<Integer> target, int uid) {
        Set<Integer> added = new TreeSet<>(target);
        added.removeAll(current);
        Set<Integer> removed = new TreeSet<>(current);
        removed.removeAll(target);
        if (added.isEmpty() && removed.isEmpty()) return; // 无变更时不做任何写入
        if (!removed.isEmpty()) userRoleDao.deleteByUserIdAndRoleIdIn(userId, removed);
        if (added.isEmpty()) return;
        long time = System.currentTimeMillis();
        List<UserRole> list = new ArrayList<>(added.size());
        for (Integer roleId : added) {
            list.add(UserRole.builder().userId(userId).roleId(roleId).createdTime(time).createdUid(uid).build());
        }
        userRoleDao.saveAll(list);
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
            item.retain("id", "serial", "name", "status");
        }
        return json;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(userDao, param, (Specification<User>) (root, query, cb) -> {
            SpecificationHelper<User> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id").deleted();
            helper.likes("name", "name", "serial");
            helper.likeExp("serial").likeExp("email").likeExp("phone");
            helper.equalWithIntNotEmpty("status").equal("createdIp").equal("loginIp");
            helper.betweenWithDate("createdTime").betweenWithDate("updatedTime");
            helper.betweenWithDate("loginTime").betweenWithDate("lockedTime").betweenWithDate("deletedTime");
            List<Integer> roleIds = DPUtil.parseIntList(param.get("roleIds"));
            if(!roleIds.isEmpty() && null != query) { // 按关联表子查询过滤授权角色
                var subquery = query.subquery(UserRole.class);
                var subRoot = subquery.from(UserRole.class);
                subquery.select(subRoot)
                        .where(cb.and(
                                cb.equal(subRoot.get("userId"), root.get("id")),
                                subRoot.get("roleId").in(roleIds)
                        ));
                helper.add(cb.exists(subquery));
            }
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = hide(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            fillInfo(rows, "createdUid", "updatedUid", "deletedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if(!DPUtil.empty(args.get("withRoles")) && !rows.isEmpty()) {
            Map<Integer, Set<Integer>> userRoleMap = new LinkedHashMap<>();
            Set<Integer> roleIds = new TreeSet<>();
            for (UserRole item : userRoleDao.findAllByUserIdIn(DPUtil.values(rows, Integer.class, "id"))) {
                userRoleMap.computeIfAbsent(item.getUserId(), key -> new TreeSet<>()).add(item.getRoleId());
                roleIds.add(item.getRoleId());
            }
            Map<Integer, Role> roleMap = DPUtil.list2map(roleDao.findAllById(roleIds), Integer.class, Role.class, "id");
            for (JsonNode row : rows) {
                ObjectNode item = (ObjectNode) row;
                Set<Integer> value = userRoleMap.getOrDefault(item.at("/id").asInt(), Collections.emptySet());
                item.replace("roleIds", DPUtil.toJSON(value));
                ArrayNode roles = item.putArray("roles");
                for (Integer roleId : value) {
                    Role role = roleMap.get(roleId);
                    if (null == role) continue;
                    ObjectNode node = roles.addObject();
                    node.put("id", role.getId());
                    node.put("name", role.getName());
                    node.put("status", role.getStatus());
                }
            }
        }
        return result;
    }

    @Transactional
    public boolean delete(List<Integer> ids, HttpServletRequest request) {
        boolean result = delete(userDao, ids, rbacService.uid(request));
        if(result && null != ids) { // 删除后立即失效在线会话
            for (Integer id : ids) {
                rbacService.removeSessions(id);
                rbacService.evictUserPermit(id); // 用户变更后同步清理该用户的资源缓存
            }
        }
        return result;
    }

    public ObjectNode infos(List<Integer> ids) {
        if(null == ids || ids.isEmpty()) return DPUtil.objectNode();
        List<User> users = userDao.findAllById(ids);
        ObjectNode nodes = (ObjectNode) filter(DPUtil.json2object(DPUtil.toJSON(users, ArrayNode.class), "id"));
        Set<Integer> roleIds = new TreeSet<>();
        Map<Integer, Set<Integer>> userRoleMap = new LinkedHashMap<>();
        Set<Integer> userIds = DPUtil.values(nodes, Integer.class, "id");
        if(!userIds.isEmpty()) {
            for (UserRole item : userRoleDao.findAllByUserIdIn(userIds)) {
                userRoleMap.computeIfAbsent(item.getUserId(), key -> new TreeSet<>()).add(item.getRoleId());
                roleIds.add(item.getRoleId());
            }
        }
        Map<Integer, Role> roleMap = DPUtil.list2map(roleDao.findAllById(roleIds), Integer.class, Role.class, "id");
        for (JsonNode node : nodes) {
            ObjectNode user = (ObjectNode) node;
            ObjectNode roles = user.putObject("roles");
            for (Integer roleId : userRoleMap.getOrDefault(user.at("/id").asInt(), Collections.emptySet())) {
                Role role = roleMap.get(roleId);
                if (null == role) continue;
                ObjectNode roleNode = roles.putObject(String.valueOf(role.getId()));
                roleNode.put("id", role.getId());
                roleNode.put("name", role.getName());
                roleNode.put("status", role.getStatus());
            }
        }
        return nodes;
    }

    public JsonNode fillInfo(JsonNode rows, String... properties) {
        return fillInfo("Uid", "UserInfo", rows, properties);
    }

    public JsonNode fillInfo(String fromSuffix, String toSuffix, JsonNode json, String... properties) {
        return fillInfo(userDao, Integer.class, "id", fromSuffix, toSuffix, json, properties);
    }
}
