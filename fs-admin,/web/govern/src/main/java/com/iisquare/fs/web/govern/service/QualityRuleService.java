package com.iisquare.fs.web.govern.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.dao.QualityRuleDao;
import com.iisquare.fs.web.govern.entity.QualityRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class QualityRuleService extends ServiceBase {

    @Autowired
    private QualityRuleDao ruleDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private QualityLogicService logicService;

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

    public QualityRule info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<QualityRule> info = ruleDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(DPUtil.empty(type)) return ApiUtil.result(1002, "类型异常", type);
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        int logicId = DPUtil.parseInt(param.get("logicId"));
        if(logicId < 0) {
            return ApiUtil.result(1004, "请选择所属分类", name);
        }
        QualityRule info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new QualityRule();
        }
        info.setName(name);
        info.setType(type);
        info.setLogicId(logicId);
        info.setCheckTable(DPUtil.parseString(param.get("checkTable")));
        info.setCheckColumn(DPUtil.parseString(param.get("checkColumn")));
        info.setCheckMetric(DPUtil.parseString(param.get("checkMetric")));
        info.setCheckWhere(DPUtil.parseString(param.get("checkWhere")));
        info.setCheckGroup(DPUtil.parseString(param.get("checkGroup")));
        info.setReferTable(DPUtil.parseString(param.get("referTable")));
        info.setReferColumn(DPUtil.parseString(param.get("referColumn")));
        info.setReferMetric(DPUtil.parseString(param.get("referMetric")));
        info.setReferWhere(DPUtil.parseString(param.get("referWhere")));
        info.setReferGroup(DPUtil.parseString(param.get("referGroup")));
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setSuggest(DPUtil.parseString(param.get("suggest")));
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
        info = ruleDao.save(info);
        return ApiUtil.result(0, null, info);

    }

    public <T> List<T> fillInfo(List<T> list, String ...properties) {
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, QualityRule> data = DPUtil.list2map(ruleDao.findAllById(ids), Integer.class, "id");
        return DPUtil.fillValues(list, properties, "Name", DPUtil.values(data, String.class, "name"));
    }

    public Map<String, Object> infos(Map<?, ?> param, Map<?, ?> args) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        if (ids.size() < 1) return ApiUtil.result(0, null, new ArrayList<>());
        List<QualityRule> rows = ruleDao.findAllById(ids);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withLogicInfo"))) {
            logicService.fillInfo(rows, "logicId");
        }
        return ApiUtil.result(0, null, rows);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<QualityRule> data = ruleDao.findAll((Specification<QualityRule>) (root, query, cb) -> {
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
            int logicId = DPUtil.parseInt(param.get("logicId"));
            if(!"".equals(DPUtil.parseString(param.get("logicId")))) {
                predicates.add(cb.equal(root.get("logicId"), logicId));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            String checkTable = DPUtil.trim(DPUtil.parseString(param.get("checkTable")));
            if(!DPUtil.empty(checkTable)) {
                predicates.add(cb.equal(root.get("checkTable"), checkTable));
            }
            String checkColumn = DPUtil.trim(DPUtil.parseString(param.get("checkColumn")));
            if(!DPUtil.empty(checkColumn)) {
                predicates.add(cb.equal(root.get("checkColumn"), checkColumn));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withLogicInfo"))) {
            logicService.fillInfo(rows, "logicId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        ruleDao.deleteInBatch(ruleDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<QualityRule> list = ruleDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (QualityRule item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        ruleDao.saveAll(list);
        return true;
    }

}
