package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.DashboardTemplateDao;
import com.iisquare.fs.web.bi.entity.DashboardTemplate;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.*;

/**
 * 大屏模板服务。模板显式保存布局和组件默认样式，创建大屏后独立生效。
 */
@Service
public class DashboardTemplateService extends ServiceBase {

    @Autowired
    DashboardTemplateDao dashboardTemplateDao;
    @Autowired
    DefaultRbacService rbacService;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<DashboardTemplate> data = dashboardTemplateDao.findAll(
                (Specification<DashboardTemplate>) (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.notEqual(root.get("status"), -1));
                    int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
                    if (id > 0) {
                        predicates.add(cb.equal(root.get("id"), id));
                    }
                    String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                    if (!DPUtil.empty(name)) {
                        predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                    }
                    return cb.and(predicates.toArray(new Predicate[0]));
                },
                PageRequest.of(page - 1, pageSize,
                        Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"),
                                new Sort.Order(Sort.Direction.DESC, "id"))));
        List<?> rows = data.getContent();
        if (!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status());
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public DashboardTemplate info(Integer id) {
        if (null == id || id < 1) return null;
        return dashboardTemplateDao.findById(id).orElse(null);
    }

    /**
     * 大屏创建页只读取启用模板，避免暴露模板配置。
     */
    public List<Map<String, Object>> options() {
        List<DashboardTemplate> templates = dashboardTemplateDao.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), 1),
                Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DashboardTemplate template : templates) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", template.getId());
            item.put("name", template.getName());
            item.put("description", template.getDescription());
            result.add(item);
        }
        return result;
    }

    public Map<Integer, String> names(Collection<Integer> ids) {
        Map<Integer, String> result = new LinkedHashMap<>();
        if (null == ids || ids.isEmpty()) return result;
        for (DashboardTemplate template : dashboardTemplateDao.findAllById(ids)) {
            result.put(template.getId(), template.getName());
        }
        return result;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String description = DPUtil.parseString(param.get("description"));
        Object contentValue = param.get("content");
        String content = contentValue instanceof String
                ? DPUtil.trim((String) contentValue)
                : DPUtil.stringify(contentValue);

        if (DPUtil.empty(name)) return ApiUtil.result(1001, "模板名称不能为空", name);
        if (!status().containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        if (DPUtil.empty(content)) return ApiUtil.result(1002, "模板内容不能为空", content);
        JsonNode parsed = DPUtil.parseJSON(content);
        if (null == parsed || !parsed.isObject()) {
            return ApiUtil.result(1003, "模板内容格式异常", content);
        }

        DashboardTemplate info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new DashboardTemplate();
        }
        int count = dashboardTemplateDao.exist(name, DPUtil.parseInt(info.getId()));
        if (count > 0) return ApiUtil.result(1502, "模板名称已存在", name);

        info.setName(name);
        info.setContent(normalizeContent((ObjectNode) parsed));
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if (null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = dashboardTemplateDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    /**
     * 规范并保存显式配置的组件默认样式，不依赖画布中的示例组件。
     */
    private String normalizeContent(ObjectNode content) {
        JsonNode common = content.get("commonComponentStyle");
        content.set("commonComponentStyle", null != common && common.isObject()
                ? common.deepCopy()
                : DPUtil.objectNode());
        content.putArray("widgets");
        JsonNode configured = content.get("componentDefaults");
        if (null != configured && configured.isObject() && !configured.isEmpty()) {
            ObjectNode normalized = DPUtil.objectNode();
            configured.fields().forEachRemaining(entry -> {
                if (!DPUtil.empty(entry.getKey()) && null != entry.getValue() && entry.getValue().isObject()) {
                    normalized.set(entry.getKey(), entry.getValue().deepCopy());
                }
            });
            content.set("componentDefaults", normalized);
            return DPUtil.stringify(content);
        }
        ObjectNode defaults = DPUtil.objectNode();
        content.set("componentDefaults", defaults);
        return DPUtil.stringify(content);
    }

    public Map<String, Object> delete(List<Integer> ids, int uid) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            List<DashboardTemplate> list = dashboardTemplateDao.findAllById(ids);
            long time = System.currentTimeMillis();
            for (DashboardTemplate item : list) {
                item.setStatus(-1);
                item.setUpdatedTime(time);
                item.setUpdatedUid(uid);
            }
            dashboardTemplateDao.saveAll(list);
            return ApiUtil.result(0, null, list.size());
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除大屏模板失败", e.getMessage());
        }
    }

}
