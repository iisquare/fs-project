package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.dao.AssessDao;
import com.iisquare.fs.web.govern.dao.AssessLogDao;
import com.iisquare.fs.web.govern.entity.Assess;
import com.iisquare.fs.web.govern.entity.AssessLog;
import com.iisquare.fs.web.govern.entity.Standard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.util.*;

@Service
public class AssessService extends ServiceBase {

    @Autowired
    private AssessDao assessDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private SourceService sourceService;
    @Autowired
    private StandardService standardService;
    @Autowired
    private AssessLogDao logDao;

    public Map<?, ?> log(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<AssessLog> data = logDao.findAll((Specification<AssessLog>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int assess = DPUtil.parseInt(param.get("assess"));
            if (assess > 0) {
                predicates.add(cb.equal(root.get("assess"), assess));
            }
            String standard = DPUtil.trim(DPUtil.parseString(param.get("standard")));
            if(!DPUtil.empty(standard)) {
                predicates.add(cb.like(root.get("standard"), standard));
            }
            String source = DPUtil.trim(DPUtil.parseString(param.get("source")));
            if(!DPUtil.empty(source)) {
                predicates.add(cb.like(root.get("source"), source));
            }
            String model = DPUtil.trim(DPUtil.parseString(param.get("model")));
            if(!DPUtil.empty(model)) {
                predicates.add(cb.like(root.get("model"), model));
            }
            String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
            if(!DPUtil.empty(code)) {
                predicates.add(cb.like(root.get("code"), code));
            }
            String level = DPUtil.trim(DPUtil.parseString(param.get("level")));
            if(!DPUtil.empty(level)) {
                predicates.add(cb.like(root.get("level"), level));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "id"))));
        List<?> rows = data.getContent();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> clear(Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        if (ids.size() < 1) return ApiUtil.result(0, null, ids.size());
        List<AssessLog> list = logDao.findAllById(ids);
        logDao.deleteAll(list);
        return ApiUtil.result(0, null, ids.size());
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Assess> data = assessDao.findAll((Specification<Assess>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), name));
            }
            String source = DPUtil.trim(DPUtil.parseString(param.get("source")));
            if(!DPUtil.empty(source)) {
                predicates.add(cb.like(root.get("source"), source));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
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

    public Assess info(Integer id) {
        if(null == id || id < 1) return null;
        return assessDao.findById(id).orElse(null);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Assess info = info(id);
        if (null == info) return ApiUtil.result(1404, "信息不存在", id);
        ObjectNode result = DPUtil.toJSON(info, ObjectNode.class);
        JsonNode content = DPUtil.parseJSON(info.getContent());
        result.replace("content", null == content ? DPUtil.objectNode(): content);
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        int status = DPUtil.parseInt(param.get("status"));
        if(param.containsKey("status") || id < 1) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        Assess info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
            info.setUpdatedUid(uid);
            info.setUpdatedTime(time);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = Assess.builder().createdUid(uid).updatedUid(uid).createdTime(time).updatedTime(time).build();
        }
        if(param.containsKey("name") || null == info.getId()) info.setName(name);
        if(param.containsKey("source") || null == info.getId()) {
            info.setSource(DPUtil.trim(DPUtil.parseString(param.get("source"))));
        }
        if(param.containsKey("content") || null == info.getId()) {
            info.setContent(DPUtil.stringify(param.get("content")));
        }
        if(param.containsKey("description") || null == info.getId()) {
            info.setDescription(DPUtil.parseString(param.get("description")));
        }
        if(param.containsKey("sort") || null == info.getId()) info.setSort(DPUtil.parseInt(param.get("sort")));
        if(param.containsKey("status") || null == info.getId()) info.setStatus(status);
        assessDao.save(info);
        return info(DPUtil.buildMap("id", info.getId()));
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        if (ids.isEmpty()) return ApiUtil.result(0, null, ids.size());
        List<Assess> list = assessDao.findAllById(ids);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        for (Assess item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        assessDao.saveAll(list);
        return ApiUtil.result(0, null, ids.size());
    }

    public Map<String, Object> check(Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = info(param);
        if (ApiUtil.failed(result)) return result;
        JsonNode assess = ApiUtil.data(result, JsonNode.class);
        int assessId = assess.at("/id").asInt();
        if (1 != assess.at("/status").asInt(0)) {
            return ApiUtil.result(71001, "评估状态未启用", assess.at("/status"));
        }
        String sourceCode = assess.at("/source").asText();
        result = sourceService.info(DPUtil.buildMap("code", sourceCode));
        if (ApiUtil.failed(result)) return result;
        JsonNode source = ApiUtil.data(result, JsonNode.class);
        if (1 != source.at("/status").asInt(0)) {
            return ApiUtil.result(71002, "数据源状态未启用", source.at("/status"));
        }
        if (!"mysql".equals(source.at("/type").asText())) {
            return ApiUtil.result(71003, "数据源类型暂不支持", source.at("/type"));
        }
        assess = assess.at("/content");
        List<String> codes = new ArrayList<>();
        Map<String, Standard> standards = standardService.fromPath(
                codes, DPUtil.explode("\n", assess.at("/code").asText()));
        if (standards.isEmpty()) {
            return ApiUtil.result(71004, "无可用数据标准", source.at("/code"));
        }
        String[] includes = DPUtil.explode("\n", assess.at("/include").asText());
        String[] excludes = DPUtil.explode("\n", assess.at("/exclude").asText());
        try (Connection connection = JDBCUtil.connection(source.at("/content"))) {
            JsonNode tables = DPUtil.filterByKey(DPUtil.filterByKey(
                    JDBCUtil.tables(connection), true, includes), false, excludes);
            if (tables.isEmpty()) {
                return ApiUtil.result(71005, "无匹配元数据", assess);
            }
            long time = System.currentTimeMillis();
            List<AssessLog> logs = new ArrayList<>();
            for (JsonNode table : tables) {
                String model = table.at("/name").asText();
                for (JsonNode column : table.at("/columns")) {
                    String code = column.at("/name").asText();
                    Standard standard = standards.get(code);
                    if (null == standard) continue;
                    ;
                    ObjectNode detail = DPUtil.objectNode();
                    AssessLog.AssessLogBuilder builder = AssessLog.builder().assess(assessId).checkTime(time);
                    builder.standard(standard.getPath()).source(sourceCode).model(model).code(code).level(standard.getLevel());
                    String[] flags = DPUtil.explode(",", standard.getFlag());
                    detail.put("standardName", standard.getCode()).put("sourceName", code);
                    builder.name(DPUtil.isItemExist(flags, "unify") && !code.equals(standard.getCode()) ? 1 : 0);
                    detail.put("standardType", standard.getType()).replace("sourceType", column.at("/type"));
                    builder.type(column.at("/type").asText("").equals(standard.getType()) ? 0 : 1);
                    detail.put("standardSize", standard.getSize()).replace("sourceSize", column.at("/size"));
                    builder.size(column.at("/size").asInt(0) == standard.getSize() ? 0 : 1);
                    detail.put("standardDigit", standard.getDigit()).replace("sourceDigit", column.at("/digit"));
                    builder.digit(column.at("/digit").asInt(0) == standard.getDigit() ? 0 : 1);
                    detail.put("standardNullable", standard.getNullable()).replace("sourceNullable", column.at("/nullable"));
                    builder.nullable(column.at("/nullable").asInt(0) == standard.getNullable() ? 0 : 1);
                    AssessLog log = builder.detail(DPUtil.stringify(detail)).build();
                    if (!log.different()) continue;
                    logs.add(log);
                }
            }
            logDao.deleteByAssess(assessId);
            if (!logs.isEmpty()) logDao.saveAll(logs);
            return ApiUtil.result(0, null, logs);
        } catch (Exception e) {
            return ApiUtil.result(75001, "执行异常", ApiUtil.getStackTrace(e));
        }
    }

}
