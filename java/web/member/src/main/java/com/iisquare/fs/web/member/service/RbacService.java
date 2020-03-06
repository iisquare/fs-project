package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
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

    public static final String RESOURCE_ATTRIBUTE_KEY = "resource";

    @Autowired
    private MenuDao menuDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private RelationDao relationDao;

    @Override
    public JsonNode currentInfo(HttpServletRequest request) {
        return DPUtil.convertJSON(currentInfo(request, null));
    }

    @Override
    public boolean hasPermit(HttpServletRequest request, String module, String controller, String action) {
        JsonNode resource = resource(request, null);
        String key = keyPermit(module, controller, action);
        return resource.has(key) ? resource.get(key).asBoolean() : false;
    }

    @Override
    public boolean hasPermit(HttpServletRequest request, Map<String, Boolean> name2boolean) {
        if (null == name2boolean) return false;
        JsonNode resource = resource(request, null);
        for (Map.Entry<String, Boolean> entry : name2boolean.entrySet()) {
            String key = entry.getKey();
            if (resource.has(key) && resource.get(key).asBoolean()) return true;
        }
        return false;
    }

    @Override
    public JsonNode resource(HttpServletRequest request, Map<String, Boolean> name2boolean) {
        ObjectNode result = DPUtil.objectNode();
        int uid = DPUtil.parseInt(ServletUtil.getSession(request, "uid"));
        if(uid < 1) return result;
        result = (ObjectNode) request.getAttribute(RESOURCE_ATTRIBUTE_KEY);
        if (null == result) {
            result = loadResource(uid);
            request.setAttribute(RESOURCE_ATTRIBUTE_KEY, result);
        }
        if (null == name2boolean) return result;
        ObjectNode data = DPUtil.objectNode();
        for (Map.Entry<String, Boolean> entry : name2boolean.entrySet()) {
            String key = entry.getKey();
            data.put(key, result.has(key) ? result.get(key).asBoolean() : entry.getValue());
        }
        return result;
    }

    @Override
    public JsonNode menu(HttpServletRequest request, Integer parentId) {
        return DPUtil.convertJSON(loadMenu(request, parentId));
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
        Set<Integer> roleIds = ServiceUtil.getPropertyValues(relationDao.findAllByTypeAndAid("user_role", uid), Integer.class, "bid");
        if(roleIds.size() < 1) return result;
        Set<Integer> bids = ServiceUtil.getPropertyValues(relationDao.findAllByTypeAndAidIn("role_resource", roleIds), Integer.class, "bid");
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
        Set<Integer> roleIds = ServiceUtil.getPropertyValues(relationDao.findAllByTypeAndAid("user_role", uid), Integer.class, "bid");
        if(roleIds.size() < 1) return new ArrayList<>();
        Set<Integer> bids = ServiceUtil.getPropertyValues(relationDao.findAllByTypeAndAidIn("role_menu", roleIds), Integer.class, "bid");
        if(bids.size() < 1) return new ArrayList<>();
        List<Menu> data = menuDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("status"), 1));
                predicates.add(root.get("id").in(bids));
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, Sort.by(new Sort.Order(Sort.Direction.DESC,"sort")));
        return ServiceUtil.formatRelation(data, Menu.class, "parentId", parentId, "id", "children");
    }

}
