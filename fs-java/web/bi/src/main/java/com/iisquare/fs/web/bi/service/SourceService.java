package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.dag.core.DSCore;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.SourceDao;
import com.iisquare.fs.web.bi.entity.Source;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

@Service
public class SourceService extends ServiceBase {

    @Autowired
    private SourceDao sourceDao;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<String, Object> schema(Source info, String table, String dsl) {
        if (null == info || 1 != info.getStatus()) return ApiUtil.result(1404, "当前数据源暂不可用！", info);
        JsonNode config = DPUtil.parseJSON(info.getContent());
        if (null == config) return ApiUtil.result(1001, "解析配置信息异常！", info.getId());
        switch (info.getType()) {
            case "MySQL": return schemaMySQL(config, table, dsl);
            default: return ApiUtil.result(1002, "数据源类型暂不支持！", info.getType());
        }
    }

    public Map<String, Object> schemaMySQL(JsonNode config, String table, String sql) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(
                    config.at("/url").asText(),
                    config.at("/username").asText(),
                    config.at("/password").asText());
            ObjectNode tables;
            if (DPUtil.empty(table)) {
                tables = JDBCUtil.tables(connection);
            } else {
                tables = JDBCUtil.tables(connection, table, sql);
            }
            Iterator<JsonNode> it = tables.iterator();
            while (it.hasNext()) {
                Iterator<JsonNode> iterator = it.next().get("columns").iterator();
                while (iterator.hasNext()) {
                    ObjectNode item = (ObjectNode) iterator.next();
                    item.put("format", DSCore.jdbc2format(item.get("type").asText()));
                }
            }
            return ApiUtil.result(0, null, tables);
        } catch (SQLException e) {
            return ApiUtil.result(2001, "获取数据库连接失败！", e.getMessage());
        } finally {
            JdbcUtils.closeConnection(connection);
        }
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Source> data = sourceDao.findAll((Specification<Source>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if(id > 0) predicates.add(cb.equal(root.get("id"), id));
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
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
        if(!DPUtil.empty(config.get("withTypeText"))) {
            DPUtil.fillValues(rows, new String[]{"type"}, new String[]{"typeText"}, types());
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

    public Map<?, ?> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("MySQL", "MySQL");
        types.put("MongoDB", "MongoDB");
        return types;
    }

    public Source info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Source> info = sourceDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        String type = DPUtil.parseString(param.get("type"));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        if(param.containsKey("type")) {
            if(!types().containsKey(type)) return ApiUtil.result(1002, "数据类型参数异常", type);
        }
        if(param.containsKey("status")) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        Source info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Source();
        }
        if(param.containsKey("name") || null == info.getId()) info.setName(name);
        if(param.containsKey("type") || null == info.getId()) info.setType(type);
        if(param.containsKey("content") || null == info.getId()) info.setContent(content);
        if(param.containsKey("description") || null == info.getId()) info.setDescription(description);
        if(param.containsKey("sort") || null == info.getId()) info.setSort(sort);
        if(param.containsKey("status") || null == info.getId()) info.setStatus(status);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = sourceDao.save(info);
        return ApiUtil.result(0, null, info);

    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Source> list = sourceDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Source item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        sourceDao.saveAll(list);
        return true;
    }

}
