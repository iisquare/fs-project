package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.core.rbac.PermitInterceptor;
import com.iisquare.fs.web.core.rbac.RbacServiceBase;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.dao.RelationDao;
import com.iisquare.fs.web.member.dao.ResourceDao;
import com.iisquare.fs.web.member.entity.Menu;
import com.iisquare.fs.web.member.entity.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
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

    @Override
    public <T> List<T> fillUserInfo(List<T> list, String... properties) {
        return userService.fillInfo(list, properties);
    }

    @Override
    public ArrayNode fillUserInfo(ArrayNode array, String... properties) {
        return userService.fillInfo(array, properties);
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
    public JsonNode menu(HttpServletRequest request, Integer parentId) {
        return DPUtil.toJSON(loadMenu(request, parentId));
    }

    @Override
    public Map<String, String> setting(String type, List<String> include, List<String> exclude) {
        return settingService.get(type, include, exclude);
    }

    @Override
    public int setting(String type, Map<String, String> data) {
        return settingService.set(type, data);
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

    private ObjectNode loadResource(int uid) {
        ObjectNode result = DPUtil.objectNode();
        Set<Integer> roleIds = DPUtil.values(relationDao.findAllByTypeAndAid("user_role", uid), Integer.class, "bid");
        if(roleIds.size() < 1) return result;
        Set<Integer> bids = DPUtil.values(relationDao.findAllByTypeAndAidIn("role_resource", roleIds), Integer.class, "bid");
        if(bids.size() < 1) return result;
        List<Resource> data = resourceDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("status"), 1));
                predicates.add(root.get("id").in(bids));
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        });
        for (Resource item : data) {
            result.put(keyPermit(item.getModule(), item.getController(), item.getAction()), true);
        }
        return result;
    }

    public List<Menu> loadMenu(HttpServletRequest request, Integer parentId) {
        int uid = DPUtil.parseInt(ServletUtil.getSession(request, "uid"));
        if(uid < 1) return new ArrayList<>();
        Set<Integer> roleIds = DPUtil.values(relationDao.findAllByTypeAndAid("user_role", uid), Integer.class, "bid");
        if(roleIds.size() < 1) return new ArrayList<>();
        Set<Integer> bids = DPUtil.values(relationDao.findAllByTypeAndAidIn("role_menu", roleIds), Integer.class, "bid");
        if(bids.size() < 1) return new ArrayList<>();
        List<Menu> data = menuDao.findAll((Specification<Menu>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            predicates.add(root.get("id").in(bids));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.asc("id")));
        return DPUtil.formatRelation(data, Menu.class, "parentId", parentId, "id", "children");
    }

}
