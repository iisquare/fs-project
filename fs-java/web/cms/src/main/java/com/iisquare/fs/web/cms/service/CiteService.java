package com.iisquare.fs.web.cms.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.cms.dao.CiteDao;
import com.iisquare.fs.web.cms.entity.Cite;
import com.iisquare.fs.web.cms.mvc.Configuration;
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
public class CiteService extends ServiceBase {

    @Autowired
    private CiteDao citeDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private Configuration configuration;

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

    public ObjectNode infos(List<String> names) {
        ObjectNode result = DPUtil.objectNode();
        if (null == names || names.size() < 1) return result;
        List<Cite> list = citeDao.findAllByNameIn(names);
        for (Cite item : list) {
            if (1 != item.getStatus()) continue;
            ObjectNode node = result.putObject(String.valueOf(item.getId()));
            node.put("id", item.getId());
            node.put("name", item.getName());
        }
        return result;
    }

    public Cite info(String name) {
        if(DPUtil.empty(name)) return null;
        return citeDao.findByName(name);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Cite info = info(name);
        if(null == info) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Cite();
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
        }
        info.setName(name);
        info.setCover(DPUtil.trim(DPUtil.parseString(param.get("cover"))));
        info.setTitle(DPUtil.trim(DPUtil.parseString(param.get("title"))));
        info.setKeyword(DPUtil.trim(DPUtil.parseString(param.get("keyword"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = citeDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, -1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Specification spec = new Specification() {
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
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };
        List<?> rows = null;
        long total = 0;
        switch (pageSize) {
            case 0:
                total = citeDao.count(spec);
                break;
            case -1:
                total = citeDao.count(spec);
                rows = citeDao.findAll(spec);
                break;
            default:
                Page<?> data = citeDao.findAll(spec, PageRequest.of(page - 1, pageSize, sort));
                rows = data.getContent();
                total = data.getTotalElements();
        }
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        citeDao.deleteInBatch(citeDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Cite> list = citeDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Cite item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        citeDao.saveAll(list);
        return true;
    }

}
