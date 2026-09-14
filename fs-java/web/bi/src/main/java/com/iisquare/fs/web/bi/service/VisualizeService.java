package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.VisualizeDao;
import com.iisquare.fs.web.bi.entity.Visualize;
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
public class VisualizeService extends ServiceBase {

    @Autowired
    VisualizeDao visualizeDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    DatasetService datasetService;

    public Map<String, Object> search(Integer datasetId, JsonNode preview, JsonNode level) {
        if (null == preview || !preview.isObject()) {
            return ApiUtil.result(1001, "配置信息异常", null);
        }
        Map<String, Object> result = datasetService.dataset(datasetId);
        if (ApiUtil.failed(result)) return result;
        try {
            return axis(datasetService.from(ApiUtil.data(result, JsonNode.class)), preview, level);
        } catch (Exception e) {
            return ApiUtil.result(30500, e.getMessage(), null);
        }
    }

    /**
     * 基于 Trino 完成报表聚合运算：钻取层级过滤、维度取值及度量聚合。
     * 维度取值与全部度量合并为单次分组聚合查询（FILTER 维度合并为 union all），
     * 度量自身的过滤条件通过条件聚合内联，避免逐取值、逐度量的 N+1 次查询及数据集重复执行。
     */
    private Map<String, Object> axis(String from, JsonNode options, JsonNode levels) throws SQLException {
        if (null == levels || !levels.isArray()) levels = DPUtil.arrayNode();
        String baseFilter = AggregationUtil.filter(options.at("/filter"), null);
        String drillFilter = levelsFilter(options, levels);
        JsonNode bucket = options.at("/axis/buckets").get(levels.size());
        if (null == bucket || !bucket.isObject()) {
            return ApiUtil.result(61001, "获取所在层级维度配置异常", null);
        }
        ObjectNode axis = DPUtil.objectNode();
        ObjectNode x = axis.putObject("x");
        String aggregation = bucket.at("/aggregation").asText("");
        String interval = bucket.at("/interval").asText("");
        x.put("aggregation", aggregation).put("interval", interval);
        x.put("label", bucket.at("/label").asText()); // 层级名称
        ArrayNode data = x.putArray("data");
        ArrayNode y = axis.putArray("y");
        List<String> expressions = new ArrayList<>(); // 度量聚合表达式，与 y 数组下标一一对应
        Iterator<JsonNode> metrics = options.at("/axis/metrics").iterator();
        while (metrics.hasNext()) {
            JsonNode metric = metrics.next();
            ObjectNode item = y.addObject();
            item.put("label", metric.at("/label").asText());
            item.putArray("data");
            String metricFilter = AggregationUtil.filter(metric.at("/filter"), null);
            expressions.add(AggregationUtil.metric(
                    metric.at("/aggregation").asText(""), metric.at("/field").asText(""), metricFilter));
        }
        ArrayNode rows;
        switch (aggregation) {
            case "TERM":
            case "HISTOGRAM":
            case "DATE_HISTOGRAM": {
                String field = bucket.at("/field").asText();
                if (DPUtil.empty(field)) return ApiUtil.result(61003, "维度字段配置异常", null);
                field = axisField(field, aggregation, interval);
                String sql = "select " + field + " as \"__value\"" + metricSelect(expressions)
                        + " from " + from + AggregationUtil.where(baseFilter, drillFilter)
                        + " group by " + field + " order by \"__value\" asc";
                rows = datasetService.rows(sql);
                for (JsonNode row : rows) {
                    JsonNode value = row.get("__value");
                    data.add(null == value ? NullNode.instance : value); // 空值保留为 null，避免展示为 "null" 字符串
                    fillMetrics(y, row);
                }
                break;
            }
            case "FILTER": {
                List<String> unions = new ArrayList<>(); // 各过滤条件为一条分支，排序字段保证前端展示顺序
                int index = 0;
                Iterator<JsonNode> filters = bucket.at("/filters").iterator();
                while (filters.hasNext()) {
                    JsonNode item = filters.next();
                    if (expressions.isEmpty()) { // 无度量时仅返回桶标签，避免非聚合查询产生重复行
                        data.add(item.at("/label").asText());
                        continue;
                    }
                    String filter = AggregationUtil.filter(item.at("/filter"), null);
                    String sql = "select " + index + " as \"__rank\", "
                            + AggregationUtil.literal(item.at("/label").asText()) + " as \"__value\""
                            + metricSelect(expressions)
                            + " from " + from + AggregationUtil.where(baseFilter, drillFilter, filter);
                    unions.add(sql);
                    index++;
                }
                if (unions.isEmpty()) break;
                rows = datasetService.rows(String.join(" union all ", unions) + " order by \"__rank\" asc");
                for (JsonNode row : rows) {
                    data.add(row.at("/__value").asText(""));
                    fillMetrics(y, row);
                }
                break;
            }
            default:
                return ApiUtil.result(61002, "维度类型暂不支持", aggregation);
        }
        axis.put("xSize", options.at("/axis/buckets").size());
        axis.put("ySize", options.at("/axis/metrics").size());
        axis.replace("levels", levels); // 请求的钻取历史
        return ApiUtil.result(0, null, axis);
    }

    /**
     * 度量聚合表达式转为查询字段片段，别名 metric_0、metric_1 与 y 数组下标一一对应
     */
    private String metricSelect(List<String> expressions) {
        List<String> result = new ArrayList<>();
        for (int index = 0; index < expressions.size(); index++) {
            result.add(String.format(", %s as \"metric_%d\"", expressions.get(index), index));
        }
        return String.join("", result);
    }

    /**
     * 将查询行中的度量结果按顺序回填至 y 数组
     */
    private void fillMetrics(ArrayNode y, JsonNode row) {
        for (int index = 0; index < y.size(); index++) {
            JsonNode value = row.get("metric_" + index);
            ((ArrayNode) y.get(index).get("data")).add(null == value ? NullNode.instance : value);
        }
    }

    /**
     * 钻取历史转换为过滤条件
     */
    private String levelsFilter(JsonNode options, JsonNode levels) {
        List<String> result = new ArrayList<>();
        JsonNode buckets = options.at("/axis/buckets");
        int levelIndex = 0;
        Iterator<JsonNode> iterator = levels.iterator();
        while (iterator.hasNext()) {
            JsonNode level = iterator.next();
            JsonNode bucket = buckets.get(levelIndex);
            if (null == bucket) throw new RuntimeException("获取层级维度条件失败");
            String aggregation = bucket.at("/aggregation").asText("");
            switch (aggregation) {
                case "TERM":
                case "HISTOGRAM":
                case "DATE_HISTOGRAM":
                    String field = bucket.at("/field").asText();
                    if (DPUtil.empty(field)) throw new RuntimeException("维度字段配置异常");
                    field = axisField(field, aggregation, bucket.at("/interval").asText(""));
                    JsonNode value = level.at("/x");
                    if (null != value && value.isNull()) { // 空值钻取条件为 is null
                        result.add(String.format("((%s) IS NULL)", field));
                    } else {
                        result.add(String.format("((%s)=%s)", field, AggregationUtil.literal(value.asText(""))));
                    }
                    break;
                case "FILTER":
                    JsonNode item = bucket.at("/filters").get(level.at("/index").asInt(-1));
                    if (null == item) throw new RuntimeException("维度过滤配置异常");
                    String filter = AggregationUtil.filter(item.at("/filter"), null);
                    if (!DPUtil.empty(filter)) result.add("(" + filter + ")");
                    break;
                default:
                    throw new RuntimeException("维度类型暂不支持");
            }
            levelIndex++;
        }
        return result.isEmpty() ? null : DPUtil.implode(" AND ", result.toArray(new String[0]));
    }

    private String axisField(String field, String aggregation, String interval) {
        String result = AggregationUtil.identifier(field);
        if ("HISTOGRAM".equals(aggregation)) {
            int divider = DPUtil.parseInt(interval);
            if (Math.abs(divider) > 1) result = String.format("floor(%s / %d)", result, divider);
        } else if ("DATE_HISTOGRAM".equals(aggregation)) {
            result = AggregationUtil.date(result, interval);
        }
        return result;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Visualize> data = visualizeDao.findAll((Specification<Visualize>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            int datasetId = DPUtil.parseInt(param.get("datasetId"));
            if(!"".equals(DPUtil.parseString(param.get("datasetId")))) {
                predicates.add(cb.equal(root.get("datasetId"), datasetId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"), new Sort.Order(Sort.Direction.DESC, "id"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withDatasetInfo"))) {
            Set<Integer> datasetIds = DPUtil.values(rows, Integer.class, "datasetId");
            DPUtil.fillValues(rows, new String[]{"datasetId"}, "Name", datasetService.names(datasetIds));
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

    public Visualize info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Visualize> info = visualizeDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        int datasetId = DPUtil.parseInt(param.get("datasetId"));
        String type = DPUtil.parseString(param.get("type"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        if(param.containsKey("status")) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        Visualize info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Visualize();
        }
        if(param.containsKey("datasetId") || null == info.getId()) info.setDatasetId(datasetId);
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
        info = visualizeDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Visualize> list = visualizeDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Visualize item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        visualizeDao.saveAll(list);
        return true;
    }

}
