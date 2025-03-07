package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.oa.dao.FormRegularDao;
import com.iisquare.fs.web.oa.entity.FormRegular;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class FormRegularService extends ServiceBase {

    @Autowired
    private FormRegularDao formRegularDao;
    @Autowired
    private DefaultRbacService rbacService;

    public ObjectNode all() {
        List<FormRegular> all = formRegularDao.findAll((Specification<FormRegular>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")));
        ObjectNode result = DPUtil.objectNode();
        for (FormRegular regular : all) {
            ObjectNode node = result.putObject(regular.getName());
            node.put("name", regular.getName());
            node.put("label", regular.getLabel());
            node.put("regex", regular.getRegex());
            node.put("tooltip", regular.getTooltip());
        }
        return result;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<?> data = formRegularDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.notEqual(root.get("status"), -1));
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.equal(root.get("name"), name));
                }
                String label = DPUtil.trim(DPUtil.parseString(param.get("label")));
                if(!DPUtil.empty(label)) {
                    predicates.add(cb.like(root.get("label"), "%" + label + "%"));
                }
                String tooltip = DPUtil.trim(DPUtil.parseString(param.get("tooltip")));
                if(!DPUtil.empty(tooltip)) {
                    predicates.add(cb.like(root.get("tooltip"), "%" + tooltip + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
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

    public FormRegular info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<FormRegular> info = formRegularDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        String label = DPUtil.parseString(param.get("label"));
        String regex = DPUtil.parseString(param.get("regex"));
        String tooltip = DPUtil.parseString(param.get("tooltip"));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String description = DPUtil.parseString(param.get("description"));
        FormRegular info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
            if(param.containsKey("name")) {
                if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
                info.setName(name);
            }
            if(param.containsKey("sort")) info.setSort(sort);
            if(param.containsKey("status")) {
                if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
                info.setStatus(status);
            }
            if(param.containsKey("description")) info.setDescription(description);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new FormRegular();
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
            info.setName(name);
            info.setSort(sort);
            if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
            info.setStatus(status);
            info.setDescription(description);
        }
        if (DPUtil.empty(label)) return ApiUtil.result(1005, "标签异常", label);
        info.setLabel(label);
        info.setRegex(regex);
        info.setTooltip(tooltip);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = formRegularDao.save(info);
        return ApiUtil.result(null == info ? 500 : 0, null, info);
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<FormRegular> list = formRegularDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (FormRegular item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        formRegularDao.saveAll(list);
        return true;
    }

}
