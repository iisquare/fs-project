package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.MatrixDao;
import com.iisquare.fs.web.bi.entity.Matrix;
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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

@Service
public class MatrixService extends ServiceBase {

    @Autowired
    MatrixDao matrixDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    DatasetService datasetService;

    public Map<String, Object> search(Integer datasetId, JsonNode preview) {
        if (null == preview || !preview.isObject()) {
            return ApiUtil.result(1001, "配置信息异常", null);
        }
        Map<String, Object> result = datasetService.dataset(datasetId);
        if (ApiUtil.failed(result)) return result;
        try {
            return aggregation(datasetService.from(ApiUtil.data(result, JsonNode.class)), preview);
        } catch (Exception e) {
            return ApiUtil.result(30500, e.getMessage(), null);
        }
    }

    /**
     * 基于 Trino 完成矩阵聚合运算：维度去重取值、层级取值、度量聚合及矩阵树构建。
     */
    private Map<String, Object> aggregation(String from, JsonNode options) throws SQLException {
        String filter = AggregationUtil.filter(options.at("/filter"), null);
        ArrayNode buckets = AggregationUtil.enabled(options.at("/aggregation/buckets"));
        ArrayNode metrics = AggregationUtil.enabled(options.at("/aggregation/metrics"));
        ObjectNode agg = DPUtil.objectNode();
        ArrayNode bucketSchema = agg.putArray("buckets");
        ArrayNode metricSchema = agg.putArray("metrics");
        List<String> bucketAliases = new ArrayList<>(); // 带别名的维度列
        List<String> bucketColumns = new ArrayList<>(); // 原始维度列，用于分组
        List<String> bucketSorts = new ArrayList<>();
        List<String> bucketKeys = new ArrayList<>(); // 维度别名，用于分组结果去重
        List<String> bucketDirections = new ArrayList<>(); // 各维度排序方向，用于内存重排
        for (int index = 0; index < buckets.size(); index++) {
            JsonNode bucket = buckets.get(index);
            String key = "bucket_" + index;
            String name = bucket.at("/name").asText();
            if (DPUtil.empty(name)) return ApiUtil.result(71001, "维度字段配置异常", bucket);
            String column = AggregationUtil.identifier(name);
            bucketColumns.add(column);
            bucketAliases.add(column + " as " + AggregationUtil.identifier(key));
            bucketKeys.add(key); // 维度别名，用于分组结果去重
            String direction = "asc".equals(bucket.at("/sort").asText("asc")) ? "asc" : "desc";
            bucketDirections.add(direction);
            bucketSorts.add(AggregationUtil.identifier(key) + " " + direction);
            bucketSchema.addObject().put("key", key).put("label", bucket.at("/title").asText(name));
        }
        if (bucketAliases.isEmpty()) return ApiUtil.result(71002, "无有效维度字段", null);
        List<String> metricAliases = new ArrayList<>();
        for (int index = 0; index < metrics.size(); index++) {
            JsonNode metric = metrics.get(index);
            String key = "metric_" + index;
            String name = metric.at("/name").asText();
            if (DPUtil.empty(name)) return ApiUtil.result(72001, "度量字段配置异常", metric);
            metricAliases.add(AggregationUtil.aggregation(metric.at("/aggregation").asText(""), name) + " as " + AggregationUtil.identifier(key));
            metricSchema.addObject().put("key", key).put("label", metric.at("/title").asText(name));
        }
        if (metricAliases.isEmpty()) return ApiUtil.result(72002, "无有效度量字段", null);
        ArrayNode levels = AggregationUtil.enabled(options.at("/aggregation/levels"));
        agg.replace("matrix", matrix(from, filter, levels, bucketAliases, bucketColumns, bucketSorts,
                bucketKeys, bucketDirections, metricAliases, agg));
        return ApiUtil.result(0, null, agg);
    }

    /**
     * 构建矩阵树与度量分组。
     * 原先按层级逐级、逐取值递归查询（N+1 次 SQL），此处合并为单次分组聚合查询后在内存中组装，
     * 并将层级表头按配置顺序显式生成，避免缺省标题导致的表头缺失；
     * 矩阵行同样从分组结果中按维度组合去重生成，仅执行一次数据集查询。
     */
    private ArrayNode matrix(String from, String filter, ArrayNode levels,
            List<String> bucketAliases, List<String> bucketColumns, List<String> bucketSorts,
            List<String> bucketKeys, List<String> bucketDirections,
            List<String> metricAliases, ObjectNode agg) throws SQLException {
        ArrayNode matrix = DPUtil.arrayNode();
        ArrayNode y = agg.putArray("y");
        ArrayNode levelSchema = agg.putArray("levels");
        List<String> levelColumns = new ArrayList<>(); // 原始层级列，用于分组
        List<String> levelAliases = new ArrayList<>(); // 带别名的层级列
        List<String> levelSorts = new ArrayList<>();
        for (int index = 0; index < levels.size(); index++) {
            JsonNode level = levels.get(index);
            String expression = level.at("/expression").asText("");
            String name = level.at("/name").asText("");
            if (DPUtil.empty(expression)) {
                if (DPUtil.empty(name)) throw new RuntimeException("层级字段配置异常");
                expression = AggregationUtil.identifier(name);
            } else {
                expression = AggregationUtil.quote(expression);
            }
            String key = "level_" + index;
            levelColumns.add(expression);
            levelAliases.add(expression + " as " + AggregationUtil.identifier(key));
            levelSorts.add(AggregationUtil.identifier(key)
                    + ("asc".equals(level.at("/sort").asText("asc")) ? " asc" : " desc"));
            // 展示名称优先取 title，缺省回退字段名，保证表头不丢失
            levelSchema.addObject().put("label", level.at("/title").asText(name));
        }
        if (levelColumns.isEmpty()) { // 无层级时退化为按维度分组的单组数据
            String sql = "select " + String.join(", ", bucketAliases) + ", " + String.join(", ", metricAliases)
                    + " from " + from + AggregationUtil.where(filter)
                    + " group by " + String.join(", ", bucketColumns)
                    + " order by " + String.join(", ", bucketSorts);
            ArrayNode rows = datasetService.rows(sql);
            agg.replace("x", distinctBuckets(rows, bucketKeys, bucketDirections));
            ObjectNode collect = y.addObject();
            collect.replace("roads", DPUtil.arrayNode());
            collect.replace("metrics", rows);
            return matrix;
        }
        String sql = "select " + String.join(", ", levelAliases) + ", " + String.join(", ", bucketAliases)
                + ", " + String.join(", ", metricAliases)
                + " from " + from + AggregationUtil.where(filter)
                + " group by " + String.join(", ", levelColumns) + ", " + String.join(", ", bucketColumns)
                + " order by " + String.join(", ", levelSorts) + ", " + String.join(", ", bucketSorts);
        ArrayNode rows = datasetService.rows(sql);
        agg.replace("x", distinctBuckets(rows, bucketKeys, bucketDirections));
        Map<String, ObjectNode> nodeMap = new HashMap<>(); // 层级路径 -> 树节点
        Map<String, ObjectNode> groupMap = new LinkedHashMap<>(); // 层级路径 -> 度量分组
        for (JsonNode row : rows) {
            ArrayNode children = matrix;
            StringBuilder path = new StringBuilder();
            List<String> roads = new ArrayList<>();
            for (int index = 0; index < levelColumns.size(); index++) {
                String value = row.at("/" + levelAlias(index)).asText(""); // 空值显示为空字符串，避免展示为 "null" 字符串
                roads.add(value);
                path.append('#').append(value);
                String nodeKey = path.toString();
                ObjectNode node = nodeMap.get(nodeKey);
                if (null == node) {
                    node = children.addObject();
                    node.put("label", value);
                    node.putArray("children");
                    nodeMap.put(nodeKey, node);
                }
                children = (ArrayNode) node.get("children");
            }
            String roadKey = path.toString();
            ObjectNode collect = groupMap.get(roadKey);
            if (null == collect) {
                collect = y.addObject();
                collect.replace("roads", DPUtil.toJSON(roads));
                collect.putArray("metrics");
                groupMap.put(roadKey, collect);
            }
            ((ArrayNode) collect.get("metrics")).add(row);
        }
        return matrix;
    }

    /**
     * 从分组聚合结果中按维度组合去重生成矩阵行，并按维度排序配置在内存中重排，
     * 复刻独立去重查询的排序语义，避免对数据集执行第二次查询。
     */
    private ArrayNode distinctBuckets(ArrayNode rows, List<String> bucketKeys, List<String> bucketDirections) {
        Map<String, ObjectNode> refer = new LinkedHashMap<>();
        for (JsonNode row : rows) {
            StringBuilder key = new StringBuilder();
            for (String bucketKey : bucketKeys) {
                key.append('#').append(row.at("/" + bucketKey).asText("null"));
            }
            String unique = key.toString();
            if (refer.containsKey(unique)) continue;
            ObjectNode item = DPUtil.objectNode();
            for (String bucketKey : bucketKeys) {
                JsonNode value = row.get(bucketKey);
                item.replace(bucketKey, null == value ? NullNode.instance : value);
            }
            refer.put(unique, item);
        }
        List<ObjectNode> list = new ArrayList<>(refer.values());
        list.sort(bucketComparator(bucketKeys, bucketDirections));
        ArrayNode result = DPUtil.arrayNode();
        list.forEach(result::add);
        return result;
    }

    /**
     * 维度取值比较器，逐维度按配置方向比较，复刻 SQL 排序语义
     */
    private Comparator<ObjectNode> bucketComparator(List<String> bucketKeys, List<String> bucketDirections) {
        return (a, b) -> {
            for (int index = 0; index < bucketKeys.size(); index++) {
                int value = compareValue(a.get(bucketKeys.get(index)), b.get(bucketKeys.get(index)));
                if ("desc".equals(bucketDirections.get(index))) value = -value;
                if (0 != value) return value;
            }
            return 0;
        };
    }

    /**
     * 单值比较：数值按数值比较，其余按字符串比较，NULL 视为最大值（正序置后、倒序置前）
     */
    private int compareValue(JsonNode a, JsonNode b) {
        boolean aNull = null == a || a.isNull() || a.isMissingNode();
        boolean bNull = null == b || b.isNull() || b.isMissingNode();
        if (aNull || bNull) return aNull ? (bNull ? 0 : 1) : -1;
        if (a.isNumber() && b.isNumber()) {
            return new BigDecimal(a.asText()).compareTo(new BigDecimal(b.asText()));
        }
        return a.asText().compareTo(b.asText());
    }

    private String levelAlias(int index) {
        return "level_" + index;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Matrix> data = matrixDao.findAll((Specification<Matrix>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
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

    public Matrix info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Matrix> info = matrixDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        int datasetId = DPUtil.parseInt(param.get("datasetId"));
        String content = DPUtil.parseString(param.get("content"));
        String description = DPUtil.parseString(param.get("description"));
        if(param.containsKey("name") || id < 1) {
            if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        }
        if(param.containsKey("status")) {
            if(!status("default").containsKey(status)) return ApiUtil.result(1004, "状态参数异常", status);
        }
        Matrix info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Matrix();
        }
        if(param.containsKey("datasetId") || null == info.getId()) info.setDatasetId(datasetId);
        if(param.containsKey("name") || null == info.getId()) info.setName(name);
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
        info = matrixDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.isEmpty()) return false;
        List<Matrix> list = matrixDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Matrix item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        matrixDao.saveAll(list);
        return true;
    }

}
