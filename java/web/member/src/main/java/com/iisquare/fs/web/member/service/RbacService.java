package com.iisquare.fs.web.member.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
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
public class RbacService extends ServiceBase {

    @Autowired
    private MenuDao menuDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private RelationDao relationDao;

    public Map<String, Object> currentInfo(HttpServletRequest request, Map<?, ?> info) {
        Map<String, Object> result = ServletUtil.getSessionMap(request);
        if(null == info) return result;
        for (Map.Entry<?, ?> entry : info.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        ServletUtil.setSession(request, result);
        return result;
    }

    public boolean hasPermit(HttpServletRequest request, String module, String controller, String action) {
        Map<String, Map<String, Set<String>>> result = resource(request);
        Map<String, Set<String>> controllers = result.get(module);
        if(null == controllers) return false;
        Set<String> actions = controllers.get(null == controller ? "" : controller);
        if(null == actions) return false;
        return actions.contains(null == action ? "" : action);
    }

    public Map<String, Map<String, Set<String>>> resource(HttpServletRequest request) {
        int uid = DPUtil.parseInt(ServletUtil.getSession(request, "uid"));
        if(uid < 1) return new HashMap<>();
        Map<String, Map<String, Set<String>>> result = (Map<String, Map<String, Set<String>>>) request.getAttribute("resource");
        if(null != result) return result;
        Set<Integer> roleIds = ServiceUtil.getPropertyValues(relationDao.findAllByTypeAndAid("user_role", uid), Integer.class, "bid");
        if(roleIds.size() < 1) return new HashMap<>();
        Set<Integer> bids = ServiceUtil.getPropertyValues(relationDao.findAllByTypeAndAidIn("role_resource", roleIds), Integer.class, "bid");
        if(bids.size() < 1) return new HashMap<>();
        List<Resource> data = resourceDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("status"), 1));
                predicates.add(root.get("id").in(bids));
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        });
        result = new HashMap<>();
        for (Resource item : data) {
            Map<String, Set<String>> controllers = result.get(item.getModule());
            if(null == controllers) {
                controllers = new HashMap<>();
                result.put(item.getModule(), controllers);
            }
            Set<String> actions = controllers.get(item.getController());
            if(null == actions) {
                actions = new HashSet<>();
                controllers.put(item.getController(), actions);
            }
            actions.add(item.getAction());
        }
        request.setAttribute("resource", result);
        return result;
    }

    public List<Menu> menu(HttpServletRequest request, Integer parentId) {
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
