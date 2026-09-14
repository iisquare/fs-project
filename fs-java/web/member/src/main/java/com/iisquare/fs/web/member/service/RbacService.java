package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.core.rbac.PermitInterceptor;
import com.iisquare.fs.web.core.rbac.RbacServiceBase;
import com.iisquare.fs.web.member.core.RedisKey;
import com.iisquare.fs.web.member.dao.ApplicationDao;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.dao.ResourceDao;
import com.iisquare.fs.web.member.dao.RoleApplicationDao;
import com.iisquare.fs.web.member.dao.RoleDao;
import com.iisquare.fs.web.member.dao.RoleMenuDao;
import com.iisquare.fs.web.member.dao.RoleResourceDao;
import com.iisquare.fs.web.member.dao.UserRoleDao;
import com.iisquare.fs.web.member.entity.Application;
import com.iisquare.fs.web.member.entity.Menu;
import com.iisquare.fs.web.member.entity.Resource;
import com.iisquare.fs.web.member.entity.Role;
import com.iisquare.fs.web.member.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class RbacService extends RbacServiceBase {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);

    @Autowired
    MenuDao menuDao;
    @Autowired
    ResourceDao resourceDao;
    @Autowired
    RoleDao roleDao;
    @Autowired
    UserRoleDao userRoleDao;
    @Autowired
    RoleApplicationDao roleApplicationDao;
    @Autowired
    RoleMenuDao roleMenuDao;
    @Autowired
    RoleResourceDao roleResourceDao;
    @Autowired
    UserService userService;
    @Autowired
    SettingService settingService;
    @Autowired
    ApplicationDao applicationDao;
    @Autowired
    RedisIndexedSessionRepository sessionRepository;
    @Autowired
    StringRedisTemplate redis;
    @Autowired
    DataLogService dataLogService;

    /**
     * 角色资源缓存有效期
     */
    public static final Duration PERMIT_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 缓存结构版本，结构变更后旧缓存自动失效
     * 4：用户缓存存储全部角色标识与锁定时间，角色缓存存储角色状态，角色有效性与账号锁定由缓存读取时判定
     * 5：用户缓存增加邮箱、手机号等身份信息，identity 所需数据全部由用户与角色缓存组装
     */
    public static final int PERMIT_CACHE_VERSION = 5;

    @Override
    public <T> List<T> fillUserInfo(List<T> list, String... properties) {
        throw new RuntimeException("replace Name to UserInfo");
    }

    @Override
    public JsonNode fillUserInfo(JsonNode json, String... properties) {
        return userService.fillInfo(json, properties);
    }

    @Override
    public JsonNode fillUserInfo(String fromSuffix, String toSuffix, JsonNode json, String... properties) {
        return userService.fillInfo(fromSuffix, toSuffix, json, properties);
    }

    @Override
    public JsonNode currentInfo(HttpServletRequest request) {
        JsonNode info = (JsonNode) request.getAttribute(PermitInterceptor.ATTRIBUTE_USER);
        if (null != info) return info;
        info = DPUtil.toJSON(currentInfo(request, null));
        request.setAttribute(PermitInterceptor.ATTRIBUTE_USER, info);
        return info;
    }

    @Override
    public JsonNode resource(HttpServletRequest request) {
        JsonNode resource = (JsonNode) request.getAttribute(PermitInterceptor.ATTRIBUTE_RESOURCE);
        if (null != resource) return resource;
        int uid = DPUtil.parseInt(ServletUtil.getSession(request, "uid"));
        resource = loadResource(uid);
        request.setAttribute(PermitInterceptor.ATTRIBUTE_RESOURCE, resource);
        return resource;
    }

    @Override
    public JsonNode menu(HttpServletRequest request) {
        int uid = DPUtil.parseInt(ServletUtil.getSession(request, "uid"));
        return loadMenu(uid);
    }

    @Override
    public Map<String, String> setting(String type, List<String> include, List<String> exclude) {
        return settingService.get(type, include, exclude);
    }

    @Override
    public int setting(String type, Map<String, String> data) {
        return settingService.set(type, data);
    }

    @Override
    public JsonNode data(HttpServletRequest request, Object params, String... permits) {
        Map<String, Object> result = dataLogService.record(
                request, logParams(request), DPUtil.toJSON(params), Arrays.asList(permits));
        return RpcUtil.data(result, false);
    }

    @Override
    public JsonNode identity(HttpServletRequest request) {
        return userService.identity(uid(request));
    }

    @Override
    public JsonNode identity(Integer uid) {
        return userService.identity(uid);
    }

    public Map<String, Object> currentInfo(HttpServletRequest request, Map<?, ?> info) {
        Map<String, Object> result = ServletUtil.getSessionMap(request);
        if(null == info) return result;
        for (Map.Entry<?, ?> entry : info.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        ServletUtil.setSession(request, result);
        Object uid = result.get("uid");
        if (!DPUtil.empty(uid)) { // 建立会话索引，便于批量失效同一用户的所有会话
            ServletUtil.setSession(request, FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, String.valueOf(uid));
        }
        return result;
    }

    public int removeSessions(int uid) {
        int count = 0;
        Map<String, ? extends Session> sessions = sessionRepository.findByPrincipalName(String.valueOf(uid));
        for (String id : sessions.keySet()) {
            sessionRepository.deleteById(id);
            count++;
        }
        return count;
    }

    public Map<String, Object> session(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        Session session = sessionRepository.findById(id);
        if (null == session) return result;
        for (String name : session.getAttributeNames()) {
            result.put(name, session.getAttribute(name));
        }
        return result;
    }

    private ObjectNode loadResource(int uid) {
        ObjectNode result = DPUtil.objectNode();
        if (uid < 1) return result;
        for (ObjectNode permit : rolePermits(userRoleIds(uid))) {
            if (1 != permit.at("/status").asInt()) continue; // 角色不存在或未启用时不参与鉴权
            for (JsonNode application : permit.at("/applications")) {
                result.put(keyPermit(application.at("/serial").asText(), null, null), true);
            }
            for (JsonNode key : permit.at("/resources")) {
                result.put(key.asText(), true);
            }
        }
        return result;
    }

    /**
     * 用户缓存，仅保存用户基础信息与配置的角色，资源、菜单等在鉴权时由角色缓存组装，有效期 30 分钟
     */
    public ObjectNode userPermit(Integer uid) {
        if (null == uid || uid < 1) return DPUtil.objectNode(); // 无效标识不读取或写入缓存
        String key = RedisKey.permitUser(uid);
        JsonNode cache = DPUtil.parseJSON(redis.opsForValue().get(key));
        if(validPermit(cache)) return (ObjectNode) cache;
        ObjectNode permit = buildUserPermit(uid);
        redis.opsForValue().set(key, permit.toString(), PERMIT_CACHE_TTL);
        return permit;
    }

    /**
     * 用户基础信息与配置的全部角色，角色有效性由角色缓存中的状态决定，用户不存在、已删除或未启用时不返回角色
     */
    private ObjectNode buildUserPermit(Integer uid) {
        ObjectNode permit = DPUtil.objectNode();
        permit.put("version", PERMIT_CACHE_VERSION);
        ArrayNode roles = permit.putArray("roles");
        User user = userService.info(uid);
        if (null == user) return permit;
        permit.put("id", user.getId());
        permit.put("serial", user.getSerial());
        permit.put("name", user.getName());
        permit.put("email", DPUtil.parseString(user.getEmail()));
        permit.put("phone", DPUtil.parseString(user.getPhone()));
        permit.put("status", DPUtil.parseInt(user.getStatus()));
        permit.put("deletedTime", DPUtil.parseLong(user.getDeletedTime()));
        permit.put("lockedTime", DPUtil.parseLong(user.getLockedTime()));
        if (1 == DPUtil.parseInt(user.getStatus()) && 0 == DPUtil.parseLong(user.getDeletedTime())) {
            for (Integer roleId : userRoleDao.findRoleIdsByUserId(uid)) {
                roles.add(roleId);
            }
        }
        return permit;
    }

    /**
     * 用户配置的全部角色标识，读取用户缓存，用户不可用或账号锁定期间返回空集合
     * 角色是否有效由角色缓存中的状态决定，不影响用户缓存的结构
     */
    public Set<Integer> userRoleIds(Integer uid) {
        Set<Integer> result = new TreeSet<>();
        if (null == uid || uid < 1) return result;
        ObjectNode permit = userPermit(uid);
        if (1 != permit.at("/status").asInt() || 0 != permit.at("/deletedTime").asLong()) return result;
        if (permit.at("/lockedTime").asLong() > System.currentTimeMillis()) return result; // 锁定期间不返回角色
        for (JsonNode roleId : permit.at("/roles")) {
            result.add(roleId.asInt());
        }
        return result;
    }

    /**
     * 批量读取角色资源：一次 multiGet 取回全部角色缓存，未命中的角色按库计算后一次 pipeline 批量回写
     */
    public List<ObjectNode> rolePermits(Collection<Integer> roleIds) {
        List<Integer> ids = new ArrayList<>(roleIds);
        List<ObjectNode> result = new ArrayList<>(ids.size());
        if(ids.isEmpty()) return result;
        List<String> values = null;
        List<String> keys = new ArrayList<>(ids.size());
        for (Integer roleId : ids) {
            keys.add(RedisKey.permitRole(roleId));
        }
        values = redis.opsForValue().multiGet(keys);
        Map<Integer, ObjectNode> missed = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            String cached = (null == values || values.size() <= i) ? null : values.get(i);
            JsonNode cache = DPUtil.parseJSON(cached);
            if(validPermit(cache)) {
                result.add((ObjectNode) cache);
                continue;
            }
            ObjectNode permit = buildRolePermit(ids.get(i));
            missed.put(ids.get(i), permit);
            result.add(permit);
        }
        saveRolePermits(missed);
        return result;
    }

    /**
     * 批量回写角色缓存：multiSet 不支持过期时间，故使用 pipeline 批量 setEx，一次往返完成
     */
    private void saveRolePermits(Map<Integer, ObjectNode> permits) {
        if(null == permits || permits.isEmpty()) return;
        redis.executePipelined((RedisCallback<Object>) connection -> {
            RedisStringCommands commands = connection.stringCommands();
            for (Map.Entry<Integer, ObjectNode> entry : permits.entrySet()) {
                commands.setEx(RedisKey.permitRole(entry.getKey()).getBytes(StandardCharsets.UTF_8),
                        PERMIT_CACHE_TTL.getSeconds(),
                        entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
    }

    /**
     * 计算角色资源：已启用应用、该角色自身已授权应用范围内已启用的资源与菜单
     * 角色状态等基础信息一并写入缓存，角色不存在或未启用时仅返回状态、不解析授权
     */
    private ObjectNode buildRolePermit(Integer roleId) {
        ObjectNode permit = DPUtil.objectNode();
        permit.put("version", PERMIT_CACHE_VERSION);
        ArrayNode applications = permit.putArray("applications");
        ArrayNode resources = permit.putArray("resources");
        ArrayNode menus = permit.putArray("menus");
        Role role = roleDao.findById(roleId).orElse(null);
        if (null == role) {
            permit.put("id", DPUtil.parseInt(roleId));
            permit.put("status", -1); // 角色不存在
            return permit;
        }
        permit.put("id", role.getId());
        permit.put("name", role.getName());
        permit.put("status", DPUtil.parseInt(role.getStatus()));
        if (1 != DPUtil.parseInt(role.getStatus())) return permit; // 角色未启用时不解析授权
        Set<Integer> applicationIds = new HashSet<>(roleApplicationDao.findApplicationIdsByRoleId(roleId));
        if(applicationIds.isEmpty()) return permit;
        List<Application> list = applicationDao.findAll((Specification<Application>) (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), 1),
                root.get("id").in(applicationIds)
        ), Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("id")));
        applicationIds.clear();
        for (Application application : list) {
            applicationIds.add(application.getId());
            ObjectNode node = applications.addObject();
            node.put("id", application.getId());
            node.put("serial", application.getSerial());
            node.put("name", application.getName());
            node.put("icon", application.getIcon());
            node.put("url", application.getUrl());
            node.put("target", application.getTarget());
            node.put("description", application.getDescription());
            node.put("sort", application.getSort());
        }
        if(applicationIds.isEmpty()) return permit;
        Set<Integer> resourceIds = new HashSet<>(roleResourceDao.findResourceIdsByRoleId(roleId));
        if(!resourceIds.isEmpty()) {
            List<Resource> items = resourceDao.findAll((Specification<Resource>) (root, query, cb) -> cb.and(
                    cb.equal(root.get("status"), 1),
                    root.get("id").in(resourceIds),
                    root.get("applicationId").in(applicationIds)
            ));
            for (Resource resource : items) {
                resources.add(keyPermit(resource.getModule(), resource.getController(), resource.getAction()));
            }
        }
        Set<Integer> menuIds = new HashSet<>(roleMenuDao.findMenuIdsByRoleId(roleId));
        if(!menuIds.isEmpty()) {
            List<Menu> items = menuDao.findAll((Specification<Menu>) (root, query, cb) -> cb.and(
                    cb.equal(root.get("status"), 1),
                    root.get("id").in(menuIds),
                    root.get("applicationId").in(applicationIds)
            ), Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("id")));
            for (Menu menu : items) {
                ObjectNode node = menus.addObject();
                node.put("id", menu.getId());
                node.put("parentId", menu.getParentId());
                node.put("applicationId", menu.getApplicationId());
                node.put("name", menu.getName());
                node.put("icon", menu.getIcon());
                node.put("url", menu.getUrl());
                node.put("target", menu.getTarget());
                node.put("description", menu.getDescription());
                node.put("sort", menu.getSort());
            }
        }
        return permit;
    }

    /**
     * 用户变更后清理该用户的资源缓存
     */
    public void evictUserPermit(Integer uid) {
        if (null == uid) return;
        afterCommit(() -> deletePermit(Arrays.asList(RedisKey.permitUser(uid))));
    }

    /**
     * 角色授权变更后清理该角色的资源缓存
     * 用户缓存中仅保存角色标识，因此无需逐个清理该角色下的用户缓存
     */
    public void evictRolePermit(Integer roleId) {
        if (null == roleId) return;
        afterCommit(() -> deletePermit(Arrays.asList(RedisKey.permitRole(roleId))));
    }

    /**
     * 应用、菜单、资源等基础数据变更后清理全部角色缓存，用户缓存不受影响
     * 仅按角色数量遍历，与用户规模无关；角色标识查询延迟到事务提交后执行，缩短事务占用
     */
    public void evictAllPermit() {
        afterCommit(() -> {
            List<String> keys = new ArrayList<>();
            for (Integer roleId : roleDao.findAllIds()) {
                keys.add(RedisKey.permitRole(roleId));
            }
            deletePermit(keys);
        });
    }

    /**
     * 清理缓存，缓存异常直接抛出由上层终止执行，避免读取到已失效的授权数据
     */
    private void deletePermit(Collection<String> keys) {
        if (null == keys || keys.isEmpty()) return;
        redis.delete(keys);
    }

    /**
     * 缓存清理延迟到事务提交后执行，避免事务提交前被并发请求按旧数据重建回写；无事务环境直接执行
     * 提交后执行时异常仅记录日志：事务已提交无法回滚，缓存将在有效期后自动重建
     */
    private void afterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runnable.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error("清理资源缓存失败", e);
                }
            }
        });
    }

    /**
     * 缓存是否可用，结构版本不一致时视为失效
     */
    private boolean validPermit(JsonNode cache) {
        return null != cache && cache.isObject() && PERMIT_CACHE_VERSION == cache.at("/version").asInt();
    }

    public ArrayNode loadMenu(int uid) {
        ArrayNode result = DPUtil.arrayNode();
        Set<Integer> roleIds = userRoleIds(uid); // 优先读取用户资源缓存
        if(roleIds.isEmpty()) return result;
        // 一次 multiGet 取回全部角色缓存，按应用组装菜单树
        Map<Integer, JsonNode> applications = new LinkedHashMap<>();
        Map<Integer, Map<Integer, JsonNode>> menus = new LinkedHashMap<>();
        for (ObjectNode permit : rolePermits(roleIds)) {
            if (1 != permit.at("/status").asInt()) continue; // 角色不存在或未启用时不组装菜单
            for (JsonNode application : permit.at("/applications")) {
                if (DPUtil.empty(application.at("/url").asText())) continue; // 链接为空时不在前台展示
                applications.putIfAbsent(application.at("/id").asInt(), application);
            }
            for (JsonNode menu : permit.at("/menus")) {
                menus.computeIfAbsent(menu.at("/applicationId").asInt(), key -> new LinkedHashMap<>())
                        .putIfAbsent(menu.at("/id").asInt(), menu);
            }
        }
        if(applications.isEmpty()) return result;
        List<JsonNode> list = new ArrayList<>(applications.values());
        list.sort(RbacService::compareBySort);
        for (JsonNode application : list) {
            ObjectNode node = applicationNode(application);
            int applicationId = application.at("/id").asInt();
            Map<Integer, JsonNode> items = menus.get(applicationId);
            if(null == items || items.isEmpty()) continue;
            List<JsonNode> children = new ArrayList<>(items.values());
            children.sort(RbacService::compareBySort);
            ArrayNode nodes = DPUtil.arrayNode();
            for (JsonNode menu : children) {
                nodes.add(menuNode(menu, applicationId));
            }
            node.replace("children", DPUtil.formatRelation(nodes, "parentId", 0, "id", "children"));
            result.add(node);
        }
        return result;
    }

    /**
     * 应用节点，字段与 Application.menu() 保持一致
     */
    private static ObjectNode applicationNode(JsonNode application) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", application.at("/id").asInt());
        node.put("name", application.at("/name").asText());
        node.put("icon", application.at("/icon").asText());
        node.put("url", application.at("/url").asText());
        node.put("target", application.at("/target").asText());
        node.put("description", application.at("/description").asText());
        node.putArray("children");
        return node;
    }

    /**
     * 菜单节点，字段与 Menu.menu() 保持一致
     */
    private static ObjectNode menuNode(JsonNode menu, Integer applicationId) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", menu.at("/id").asInt());
        node.put("parentId", menu.at("/parentId").asInt());
        node.put("applicationId", applicationId);
        node.put("name", menu.at("/name").asText());
        node.put("icon", menu.at("/icon").asText());
        node.put("url", menu.at("/url").asText());
        node.put("target", menu.at("/target").asText());
        node.put("description", menu.at("/description").asText());
        node.putArray("children");
        return node;
    }

    /**
     * 按排序值倒序、标识正序比较，与列表查询的排序保持一致
     */
    private static int compareBySort(JsonNode source, JsonNode target) {
        int result = Integer.compare(target.at("/sort").asInt(), source.at("/sort").asInt());
        if (0 != result) return result;
        return Integer.compare(source.at("/id").asInt(), target.at("/id").asInt());
    }

}
