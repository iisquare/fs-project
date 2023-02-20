package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.core.Rule;
import com.iisquare.fs.web.govern.dao.QualityLogDao;
import com.iisquare.fs.web.govern.dao.QualityPlanDao;
import com.iisquare.fs.web.govern.entity.QualityLog;
import com.iisquare.fs.web.govern.entity.QualityPlan;
import com.iisquare.fs.web.govern.entity.QualityRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@Service
public class QualityPlanService extends ServiceBase {

    @Autowired
    private QualityPlanDao planDao;
    @Autowired
    private QualityLogDao logDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private QualityLogicService logicService;
    @Autowired
    private QualityRuleService ruleService;
    @Autowired
    private SourceService sourceService;

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

    public QualityPlan info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<QualityPlan> info = planDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        String source = DPUtil.trim(DPUtil.parseString(param.get("source")));
        if(DPUtil.empty(source)) return ApiUtil.result(1002, "数据源编码异常", source);
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        QualityPlan info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new QualityPlan();
        }
        info.setName(name);
        info.setSource(source);
        info.setContent(DPUtil.parseString(param.get("content")));
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
        info = planDao.save(info);
        return ApiUtil.result(0, null, info);

    }

    public <T> List<T> fillInfo(List<T> list, String ...properties) {
        Set<Integer> ids = DPUtil.values(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, QualityPlan> data = DPUtil.list2map(planDao.findAllById(ids), Integer.class, "id");
        return DPUtil.fillValues(list, properties, "Name", DPUtil.values(data, String.class, "name"));
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<QualityPlan> data = planDao.findAll((Specification<QualityPlan>) (root, query, cb) -> {
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
            String source = DPUtil.trim(DPUtil.parseString(param.get("source")));
            if(!DPUtil.empty(source)) {
                predicates.add(cb.equal(root.get("source"), source));
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
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        planDao.deleteInBatch(planDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<QualityPlan> list = planDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (QualityPlan item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        planDao.saveAll(list);
        return true;
    }

    public Map<String, Object> check(Map<?, ?> param, HttpServletRequest request) {
        QualityPlan info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) {
            return ApiUtil.result(1001, "记录不存在", param);
        }
        if (1 != info.getStatus()) {
            return ApiUtil.result(1002, "记录状态未启用", info);
        }
        Map<String, Object> result = sourceService.info(DPUtil.buildMap("code", info.getSource()));
        if (ApiUtil.failed(result)) return result;
        JsonNode source = ApiUtil.data(result, JsonNode.class);
        if (1 != source.at("/status").asInt(0)) {
            return ApiUtil.result(71002, "数据源状态未启用", source.at("/status"));
        }
        if (!"mysql".equals(source.at("/type").asText())) {
            return ApiUtil.result(71003, "数据源类型暂不支持", source.at("/type"));
        }
        Connection connection;
        try {
            connection = JDBCUtil.connection(source.at("/content"));
        } catch (SQLException e) {
            return ApiUtil.result(1003, "获取数据库连接失败", e.getMessage());
        }
        result = ruleService.infos(DPUtil.buildMap("ids", info.getContent()), DPUtil.buildMap());
        List<QualityRule> rules = ApiUtil.data(result, List.class);
        for (QualityRule rule : rules) {
            if (1 != rule.getStatus()) continue;
            result = Rule.entity(connection, rule);
            QualityLog.QualityLogBuilder builder = QualityLog.builder();
            builder.source(info.getSource()).planId(info.getId());
            builder.ruleId(rule.getId()).logicId(rule.getLogicId());
            builder.checkTable(rule.getCheckTable()).checkColumn(rule.getCheckColumn());
            builder.state(ApiUtil.code(result)).reason(ApiUtil.message(result));
            builder.createdTime(System.currentTimeMillis());
            if (ApiUtil.failed(result)) {
                builder.checkCount(0).hitCount(0).expression("");
                Object data = ApiUtil.data(result, Object.class);
                if (data instanceof Throwable) {
                    builder.expression(ApiUtil.getStackTrace((Throwable) data));
                }
            } else {
                JsonNode data = ApiUtil.data(result, JsonNode.class);
                builder.checkCount(data.at("/checkCount").asInt(0));
                builder.hitCount(data.at("/hitCount").asInt(0));
                builder.expression(data.at("/expression").asText(""));
            }
            logDao.save(builder.build());
        }
        FileUtil.close(connection);
        return ApiUtil.result(0, null, param);
    }

    public Map<?, ?> log(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<QualityLog> data = logDao.findAll((Specification<QualityLog>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String source = DPUtil.trim(DPUtil.parseString(param.get("source")));
            if(!DPUtil.empty(source)) {
                predicates.add(cb.like(root.get("source"), source));
            }
            int planId = DPUtil.parseInt(param.get("planId"));
            if (planId > 0) {
                predicates.add(cb.equal(root.get("planId"), planId));
            }
            int ruleId = DPUtil.parseInt(param.get("ruleId"));
            if (ruleId > 0) {
                predicates.add(cb.equal(root.get("ruleId"), ruleId));
            }
            int logicId = DPUtil.parseInt(param.get("logicId"));
            if (logicId > 0) {
                predicates.add(cb.equal(root.get("logicId"), logicId));
            }
            String checkTable = DPUtil.trim(DPUtil.parseString(param.get("checkTable")));
            if(!DPUtil.empty(checkTable)) {
                predicates.add(cb.like(root.get("checkTable"), checkTable));
            }
            String checkColumn = DPUtil.trim(DPUtil.parseString(param.get("checkColumn")));
            if(!DPUtil.empty(checkColumn)) {
                predicates.add(cb.like(root.get("checkColumn"), checkColumn));
            }
            int state = DPUtil.parseInt(param.get("state"));
            if(!"".equals(DPUtil.parseString(param.get("state")))) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "id"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withLogicInfo"))) {
            logicService.fillInfo(rows, "logicId");
        }
        if(!DPUtil.empty(config.get("withRuleInfo"))) {
            ruleService.fillInfo(rows, "ruleId");
        }
        if(!DPUtil.empty(config.get("withPlanInfo"))) {
            fillInfo(rows, "planId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> clear(Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        if (ids.size() < 1) return ApiUtil.result(0, null, ids.size());
        List<QualityLog> list = logDao.findAllById(ids);
        logDao.deleteAll(list);
        return ApiUtil.result(0, null, ids.size());
    }

}
