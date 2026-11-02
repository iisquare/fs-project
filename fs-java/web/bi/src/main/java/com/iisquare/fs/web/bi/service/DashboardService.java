package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.DashboardDao;
import com.iisquare.fs.web.bi.entity.Dashboard;
import com.iisquare.fs.web.bi.util.AggregationUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;

@Service
public class DashboardService extends ServiceBase {

    @Autowired
    DashboardDao dashboardDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    VisualizeService visualizeService;
    @Autowired
    DatasetService datasetService;

    /**
     * 大屏维度可以自由组合，因此聚合查询一次返回全部维度与指标。
     * 兼容旧版单维度 preview 请求，便于既有报表配置继续使用。
     */
    public Map<String, Object> search(Integer datasetId, JsonNode preview, JsonNode levels) {
        if (null != preview && "aggregate".equals(preview.at("/mode").asText())) {
            return aggregate(datasetId, preview);
        }
        return visualizeService.search(datasetId, preview, levels);
    }

    private Map<String, Object> aggregate(Integer datasetId, JsonNode preview) {
        if (null == preview || !preview.isObject()) {
            return ApiUtil.result(1001, "配置信息异常", null);
        }
        Map<String, Object> dataset = datasetService.dataset(datasetId);
        if (ApiUtil.failed(dataset)) return dataset;
        JsonNode buckets = preview.at("/axis/buckets");
        if (!buckets.isArray() || buckets.isEmpty()) {
            return ApiUtil.result(82001, "请至少配置一个维度", null);
        }
        try {
            return aggregate(datasetService.from(ApiUtil.data(dataset, JsonNode.class)), preview);
        } catch (Exception e) {
            return ApiUtil.result(30500, e.getMessage(), null);
        }
    }

    /**
     * 一次 SQL 查询完成多维度分组与多指标聚合，避免按图表重复计算。
     */
    private Map<String, Object> aggregate(String from, JsonNode options) throws SQLException {
        ArrayNode buckets = (ArrayNode) options.at("/axis/buckets");
        JsonNode metricConfig = options.at("/axis/metrics");
        ArrayNode metrics = metricConfig.isArray() ? (ArrayNode) metricConfig : DPUtil.arrayNode();
        List<String> dimensions = new ArrayList<>();
        List<String> dimensionAliases = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < buckets.size(); index++) {
            JsonNode bucket = buckets.get(index);
            String name = bucket.at("/field").asText("");
            if (DPUtil.empty(name)) return ApiUtil.result(82002, "维度字段配置异常", bucket);
            String expression = AggregationUtil.identifier(name);
            String aggregation = bucket.at("/aggregation").asText("TERM");
            String interval = bucket.at("/interval").asText("");
            if ("HISTOGRAM".equals(aggregation)) {
                int divider = DPUtil.parseInt(interval);
                if (Math.abs(divider) > 1) {
                    expression = String.format("floor(%s / %d)", expression, divider);
                }
            } else if ("DATE_HISTOGRAM".equals(aggregation)) {
                expression = AggregationUtil.date(expression, interval);
            }
            String alias = "\"__d" + index + "\"";
            dimensions.add(expression);
            dimensionAliases.add(expression + " as " + alias);
            labels.add(bucket.at("/label").asText(name));
        }
        List<String> metricsSelect = new ArrayList<>();
        for (int index = 0; index < metrics.size(); index++) {
            JsonNode metric = metrics.get(index);
            String field = metric.at("/field").asText("");
            String aggregation = metric.at("/aggregation").asText("COUNT");
            if (!"COUNT".equals(aggregation) && DPUtil.empty(field)) {
                return ApiUtil.result(82003, "指标字段配置异常", metric);
            }
            String filter = AggregationUtil.filter(metric.at("/filter"), null);
            metricsSelect.add(AggregationUtil.metric(aggregation, field, filter) + " as \"__m" + index + "\"");
        }
        String filter = AggregationUtil.filter(options.at("/filter"), null);
        int limit = ValidateUtil.filterInteger(options.at("/limit"), true, 1, 10000, 5000);
        StringBuilder sql = new StringBuilder("select ")
                .append(String.join(", ", dimensionAliases));
        if (!metricsSelect.isEmpty()) sql.append(", ").append(String.join(", ", metricsSelect));
        sql.append(" from ").append(from).append(AggregationUtil.where(filter))
                .append(" group by ").append(String.join(", ", dimensions))
                .append(" order by 1");
        for (int index = 1; index < dimensions.size(); index++) sql.append(", ").append(index + 1);
        sql.append(" limit ").append(limit);
        ArrayNode rows = datasetService.rows(sql.toString());

        ObjectNode axis = DPUtil.objectNode();
        ObjectNode x = axis.putObject("x");
        x.put("label", String.join(" / ", labels));
        ArrayNode xData = x.putArray("data");
        ArrayNode tableRows = axis.putArray("rows");
        ArrayNode y = axis.putArray("y");
        for (int index = 0; index < metrics.size(); index++) {
            y.addObject()
                    .put("label", metrics.get(index).at("/label").asText("指标 " + (index + 1)))
                    .putArray("data");
        }
        for (JsonNode row : rows) {
            List<String> values = new ArrayList<>();
            ObjectNode tableRow = tableRows.addObject();
            for (int index = 0; index < dimensions.size(); index++) {
                JsonNode value = row.get("__d" + index);
                values.add(null == value || value.isNull() ? "空值" : value.asText(""));
                tableRow.set("__d" + index, null == value ? NullNode.instance : value);
            }
            xData.add(String.join(" / ", values));
            for (int index = 0; index < metrics.size(); index++) {
                JsonNode value = row.get("__m" + index);
                tableRow.set("__m" + index, null == value ? NullNode.instance : value);
                ((ArrayNode) y.get(index).get("data")).add(null == value ? NullNode.instance : value);
            }
        }
        axis.putArray("dimensions");
        for (int index = 0; index < labels.size(); index++) {
            ((ArrayNode) axis.get("dimensions")).addObject()
                    .put("key", "__d" + index)
                    .put("label", labels.get(index));
        }
        axis.put("xSize", dimensions.size());
        axis.put("ySize", metrics.size());
        return ApiUtil.result(0, null, axis);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Dashboard> data = dashboardDao.findAll((Specification<Dashboard>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize,
                Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"), new Sort.Order(Sort.Direction.DESC, "id"))));
        List<?> rows = data.getContent();
        if (!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(config.get("withStatusText"))) {
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
            case "full":
                status.put(-1, "已删除");
                break;
            case "default":
                break;
            default:
                return null;
        }
        return status;
    }

    public Dashboard info(Integer id) {
        if (null == id || id < 1) return null;
        return dashboardDao.findById(id).orElse(null);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if (param.containsKey("name") || id < 1) {
            if (DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        if (param.containsKey("status")) {
            if (!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        Dashboard info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Dashboard();
        }
        if (param.containsKey("name") || null == info.getId()) info.setName(name);
        if (param.containsKey("content") || null == info.getId()) info.setContent(content);
        if (param.containsKey("description") || null == info.getId()) info.setDescription(description);
        if (param.containsKey("sort") || null == info.getId()) info.setSort(sort);
        if (param.containsKey("status") || null == info.getId()) info.setStatus(status);
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if (null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info = dashboardDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public Map<String, Object> delete(List<Integer> ids, int uid) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            List<Dashboard> list = dashboardDao.findAllById(ids);
            long time = System.currentTimeMillis();
            for (Dashboard item : list) {
                item.setStatus(-1);
                item.setUpdatedTime(time);
                item.setUpdatedUid(uid);
            }
            dashboardDao.saveAll(list);
            return ApiUtil.result(0, null, list.size());
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除数据大屏失败", e.getMessage());
        }
    }

}
