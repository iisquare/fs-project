package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.core.rbac.PermitInterceptor;
import com.iisquare.fs.web.core.rbac.RbacServiceBase;
import com.iisquare.fs.web.member.dao.ApplicationDao;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.dao.RelationDao;
import com.iisquare.fs.web.member.dao.ResourceDao;
import com.iisquare.fs.web.member.entity.Application;
import com.iisquare.fs.web.member.entity.Menu;
import com.iisquare.fs.web.member.entity.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class RbacService extends RbacServiceBase {

    @Autowired
    private MenuDao menuDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private UserService userService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private ApplicationDao applicationDao;
    @Autowired
    RedisIndexedSessionRepository sessionRepository;
    @Autowired
    DataLogService dataLogService;

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
        if(uid < 1) {
            resource = DPUtil.objectNode();
        } else {
            resource = loadResource(uid);
        }
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

    public Map<String, Object> currentInfo(HttpServletRequest request, Map<?, ?> info) {
        Map<String, Object> result = ServletUtil.getSessionMap(request);
        if(null == info) return result;
        for (Map.Entry<?, ?> entry : info.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        ServletUtil.setSession(request, result);
        return result;
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
        Set<Integer> roleIds = DPUtil.values(relationDao.findAllByTypeAndAid("user_role", uid), Integer.class, "bid");
        if(roleIds.size() < 1) return result;
        Set<Integer> applicationIds = DPUtil.values(relationDao.findAllByTypeAndAidIn("role_application", roleIds), Integer.class, "bid");
        if(applicationIds.size() < 1) return result;
        List<Application> applications = applicationDao.findAll((Specification<Application>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            predicates.add(root.get("id").in(applicationIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        applicationIds.clear();
        for (Application application : applications) {
            applicationIds.add(application.getId());
            result.put(keyPermit(application.getSerial(), null, null), true);
        }
        if (applicationIds.size() < 1) return result;
        Set<Integer> resourceIds = DPUtil.values(relationDao.findAllByTypeAndAidInAndCidIn("role_resource", roleIds, applicationIds), Integer.class, "bid");
        if(resourceIds.size() < 1) return result;
        List<Resource> resources = resourceDao.findAll((Specification<Resource>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            predicates.add(root.get("id").in(resourceIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        for (Resource resource : resources) {
            result.put(keyPermit(resource.getModule(), resource.getController(), resource.getAction()), true);
        }
        return result;
    }

    public ArrayNode loadMenu(int uid) {
        ArrayNode result = DPUtil.arrayNode();
        Set<Integer> roleIds = DPUtil.values(relationDao.findAllByTypeAndAid("user_role", uid), Integer.class, "bid");
        if(roleIds.size() < 1) return result;
        Set<Integer> applicationIds = DPUtil.values(relationDao.findAllByTypeAndAidIn("role_application", roleIds), Integer.class, "bid");
        if(applicationIds.size() < 1) return result;
        List<Application> applications = applicationDao.findAll((Specification<Application>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            predicates.add(root.get("id").in(applicationIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("id")));
        applicationIds.clear();
        for (Application application : applications) {
            if (DPUtil.empty(application.getUrl())) {
                continue; // 链接为空时不在前台展示
            }
            applicationIds.add(application.getId());
            result.add(application.menu());
        }
        if (applicationIds.size() < 1) return result;
        Set<Integer> menuIds = DPUtil.values(relationDao.findAllByTypeAndAidInAndCidIn("role_menu", roleIds, applicationIds), Integer.class, "bid");
        if(menuIds.size() < 1) return result;
        List<Menu> menus = menuDao.findAll((Specification<Menu>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            predicates.add(root.get("id").in(menuIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("id")));
        Map<Integer, List<Menu>> menuMap = DPUtil.list2ml(menus, Integer.class, "applicationId");
        for (JsonNode app : result) {
            int applicationId = app.at("/id").asInt();
            List<Menu> list = menuMap.get(applicationId);
            if (null == list) continue;
            ((ObjectNode) app).replace("children", formatMenu(list, 0));
        }
        return result;
    }

    public static ArrayNode formatMenu(List<Menu> list, int parentId) {
        ArrayNode result = DPUtil.arrayNode();
        for (Menu menu : list) {
            if (parentId != menu.getParentId()) continue;
            ObjectNode node = menu.menu();
            result.add(node);
            ArrayNode children = formatMenu(list, menu.getId());
            node.replace("children", children);
        }
        return result;
    }

}
