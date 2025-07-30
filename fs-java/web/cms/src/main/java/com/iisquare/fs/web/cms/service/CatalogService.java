package com.iisquare.fs.web.cms.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.cms.dao.ArticleDao;
import com.iisquare.fs.web.cms.dao.CatalogDao;
import com.iisquare.fs.web.cms.entity.Catalog;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class CatalogService extends ServiceBase {

    @Autowired
    private CatalogDao catalogDao;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private DefaultRbacService rbacService;

    public List<Catalog> tree(Map<?, ?> param, Map<?, ?> args) {
        List<Catalog> data = catalogDao.findAll((Specification<Catalog>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), -1));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(data, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(data, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        return DPUtil.formatRelation(data, Catalog.class, "parentId", 0, "id", "children");
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "关闭");
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

    public Catalog info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Catalog> info = catalogDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        int parentId = DPUtil.parseInt(param.get("parentId"));
        if(parentId < 0) {
            return ApiUtil.result(1003, "上级节点异常", name);
        } else if(parentId > 0) {
            Catalog parent = info(parentId);
            if(null == parent || !status("default").containsKey(parent.getStatus())) {
                return ApiUtil.result(1004, "上级节点不存在或已删除", name);
            }
        }
        String cover = DPUtil.trim(DPUtil.parseString(param.get("cover")));
        String title = DPUtil.trim(DPUtil.parseString(param.get("title")));
        String keyword = DPUtil.trim(DPUtil.parseString(param.get("keyword")));
        Catalog info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Catalog();
        }
        info.setName(name);
        info.setParentId(parentId);
        info.setCover(cover);
        info.setTitle(title);
        info.setKeyword(keyword);
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = catalogDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public <T> List<T> fillInfo(List<T> list, String ...properties) {
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, Catalog> data = DPUtil.list2map(catalogDao.findAllById(ids), Integer.class, "id");
        return DPUtil.fillValues(list, properties, "Name", DPUtil.values(data, String.class, "name"));
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<?> data = catalogDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                int id = DPUtil.parseInt(param.get("id"));
                if(id > 0) predicates.add(cb.equal(root.get("id"), id));
                int status = DPUtil.parseInt(param.get("status"));
                if(!"".equals(DPUtil.parseString(param.get("status")))) {
                    predicates.add(cb.equal(root.get("status"), status));
                } else {
                    predicates.add(cb.notEqual(root.get("status"), -1));
                }
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                String title = DPUtil.trim(DPUtil.parseString(param.get("title")));
                if(!DPUtil.empty(title)) {
                    predicates.add(cb.like(root.get("title"), "%" + title + "%"));
                }
                int parentId = DPUtil.parseInt(param.get("parentId"));
                if(!"".equals(DPUtil.parseString(param.get("parentId")))) {
                    predicates.add(cb.equal(root.get("parentId"), parentId));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withParentInfo"))) {
            this.fillInfo(rows, "parentId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        catalogDao.deleteInBatch(catalogDao.findAllById(ids));
        return true;
    }

    public Map<String, Object> delete(Integer id, int uid) {
        Catalog info = info(id);
        if(null == info || -1 == info.getStatus()) {
            return ApiUtil.result(404, null, id);
        }
        int childrenCount = catalogDao.childrenCount(info.getId()).intValue();
        if (childrenCount > 0) {
            return ApiUtil.result(1401, "请删除子栏目后再操作", childrenCount);
        }
        int articleCount = articleDao.countByCatalogId(info.getId()).intValue();
        if (articleCount > 0) {
            return ApiUtil.result(1403, "请处理栏目下文章后再操作", articleCount);
        }
        long time = System.currentTimeMillis();
        info.setStatus(-1);
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        info = catalogDao.save(info);
        return ApiUtil.result(0, null, info.getId());
    }

}
