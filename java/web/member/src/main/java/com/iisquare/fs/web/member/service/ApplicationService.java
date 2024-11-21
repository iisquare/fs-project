package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.member.dao.ApplicationDao;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.dao.ResourceDao;
import com.iisquare.fs.web.member.entity.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class ApplicationService extends ServiceBase {

    @Autowired
    private ApplicationDao applicationDao;
    @Autowired
    private UserService userService;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private MenuDao menuDao;
    @Autowired
    private ResourceDao resourceDao;

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

    public ObjectNode infos(List<Integer> ids) {
        ObjectNode result = DPUtil.objectNode();
        if (null == ids || ids.size() < 1) return result;
        List<Application> list = applicationDao.findAllById(ids);
        for (Application item : list) {
            if (1 != item.getStatus()) continue;
            ObjectNode node = result.putObject(String.valueOf(item.getId()));
            node.put("id", item.getId());
            node.put("name", item.getName());
        }
        return result;
    }

    public Application info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Application> info = applicationDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public <T> List<T> fillInfo(List<T> list, String ...properties) {
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, Application> data = DPUtil.list2map(applicationDao.findAllById(ids), Integer.class, "id");
        return DPUtil.fillValues(list, properties, "Name", DPUtil.values(data, String.class, "name"));
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial)) return ApiUtil.result(1001, "标识异常", serial);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1002, "名称异常", name);
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        String icon = DPUtil.trim(DPUtil.parseString(param.get("icon")));
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        String target = DPUtil.trim(DPUtil.parseString(param.get("target")));
        Application info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Application();
        }
        int count = applicationDao.exist(serial, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1003, "标识已存在", serial);
        }
        info.setSerial(serial);
        info.setName(name);
        info.setIcon(icon);
        info.setUrl(url);
        info.setTarget(target);
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
        info = applicationDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, -1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Specification<Application> spec = (Specification<Application>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if(id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), -1));
            }
            String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
            if(!DPUtil.empty(serial)) {
                predicates.add(cb.like(root.get("serial"), "%" + serial + "%"));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Application> rows = null;
        long total = 0;
        switch (pageSize) {
            case 0:
                total = applicationDao.count(spec);
                break;
            case -1:
                total = applicationDao.count(spec);
                rows = applicationDao.findAll(spec);
                break;
            default:
                Page<Application> data = applicationDao.findAll(spec, PageRequest.of(page - 1, pageSize, sort));
                rows = data.getContent();
                total = data.getTotalElements();
        }
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
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
        menuDao.deleteByApplicationIds(ids);
        resourceDao.deleteByApplicationIds(ids);
        applicationDao.deleteInBatch(applicationDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Application> list = applicationDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Application item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        applicationDao.saveAll(list);
        return true;
    }

}
