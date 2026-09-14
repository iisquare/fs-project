package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.sse.MaintainEmitter;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.bi.core.RedisKey;
import com.iisquare.fs.web.bi.dao.DatasetDao;
import com.iisquare.fs.web.bi.entity.Dataset;
import com.iisquare.fs.web.bi.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.CronRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DatasetService extends JPAServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(DatasetService.class);

    @Autowired
    DatasetDao datasetDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    TrinoService trinoService;
    @Autowired
    CronRpc cronRpc;
    @Autowired
    StringRedisTemplate redis;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("direct", "直连");
        types.put("cron", "定时同步");
        return types;
    }

    public List<String> fieldTypes() {
        return Arrays.asList(
                "string",
                "integer",
                "long",
                "float",
                "double",
                "date",
                "time",
                "datetime"
        );
    }

    public Dataset info(Integer id) {
        return info(datasetDao, id);
    }

    /**
     * 解析数据集：校验状态并返回名称及其查询语句，供矩阵、报表等运算使用。
     */
    public Map<String, Object> dataset(Integer id) {
        Dataset info = info(id);
        if (null == info || 1 != info.getStatus()) return ApiUtil.result(61001, "数据集状态异常", id);
        String sql = DPUtil.trim(info.getContent());
        if (DPUtil.empty(sql)) return ApiUtil.result(61002, "数据集查询语句不能为空", id);
        ObjectNode dataset = DPUtil.objectNode();
        dataset.put("id", info.getId());
        dataset.put("name", info.getName());
        dataset.put("sql", sql);
        return ApiUtil.result(0, null, dataset);
    }

    /**
     * 将数据集查询语句包装为可参与运算的数据来源。
     */
    public String from(JsonNode dataset) {
        String sql = dataset.at("/sql").asText("");
        while (sql.endsWith(";")) {
            sql = DPUtil.trim(sql.substring(0, sql.length() - 1));
        }
        String name = dataset.at("/name").asText("dataset");
        return String.format("(%s) as %s", sql, "\"" + name.replace("\"", "\"\"") + "\"");
    }

    /**
     * 执行数据集查询，返回 JSON 行数组。
     */
    public ArrayNode rows(String sql) throws SQLException {
        ArrayNode rows = DPUtil.arrayNode();
        try (Connection connection = trinoService.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(600);
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData meta = resultSet.getMetaData();
                int count = meta.getColumnCount();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= count; index++) {
                        row.put(meta.getColumnLabel(index), resultSet.getObject(index));
                    }
                    rows.add(DPUtil.toJSON(row));
                }
            }
        }
        return rows;
    }

    /**
     * 执行数据集聚合查询，返回第一行第一列的值。
     */
    public Object scalar(String sql) throws SQLException {
        final Object[] value = new Object[1];
        try (Connection connection = trinoService.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(600);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) value[0] = resultSet.getObject(1);
            }
        }
        return value[0];
    }

    /**
     * 获取数据集字段信息，供矩阵、报表等设计器选择字段使用。
     */
    public Map<String, Object> columns(Map<?, ?> param) {
        Integer id = DPUtil.parseInt(param.get("id"));
        Dataset info = info(id);
        if (null == info || 1 != info.getStatus()) return ApiUtil.result(61001, "数据集状态异常", id);
        ObjectNode result = DPUtil.objectNode();
        ArrayNode columns = result.putArray("columns");
        JsonNode fields = DPUtil.parseJSON(info.getFields(), k -> DPUtil.arrayNode());
        for (JsonNode field : fields) {
            String name = field.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            ObjectNode column = columns.addObject();
            column.put("name", name);
            column.put("title", field.at("/title").asText(name));
            column.put("type", field.at("/type").asText(""));
            column.put("comment", field.at("/comment").asText(""));
        }
        if (columns.isEmpty() && !DPUtil.empty(info.getContent())) { // 未维护字段配置时，按查询语句解析字段
            try {
                columns.addAll(describe(from(ApiUtil.data(dataset(id), JsonNode.class))));
            } catch (Exception e) {
                logger.warn("解析数据集字段失败, id: {}, message: {}", id, e.getMessage());
            }
        }
        return ApiUtil.result(0, null, result);
    }

    private ArrayNode describe(String from) throws SQLException {
        ArrayNode columns = DPUtil.arrayNode();
        try (Connection connection = trinoService.connection();
             PreparedStatement statement = connection.prepareStatement("select * from " + from + " limit 1")) {
            statement.setQueryTimeout(60);
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData meta = resultSet.getMetaData();
                for (int index = 1; index <= meta.getColumnCount(); index++) {
                    String name = meta.getColumnLabel(index);
                    ObjectNode column = columns.addObject();
                    column.put("name", name);
                    column.put("title", name);
                    column.put("type", meta.getColumnTypeName(index));
                    column.put("comment", "");
                }
            }
        }
        return columns;
    }

    /**
     * 获取数据集主键与名称的映射，用于列表回显引用数据集名称。
     */
    public Map<Integer, String> names(Collection<Integer> ids) {
        Map<Integer, String> result = new LinkedHashMap<>();
        if (null == ids || ids.isEmpty()) return result;
        for (Dataset info : datasetDao.findAllById(ids)) {
            result.put(info.getId(), info.getName());
        }
        return result;
    }

    public boolean isMaterialized(String type) {
        return "cron".equals(type);
    }

    public boolean changed(Dataset info, Map<?, ?> param) {
        if (null == info) return true;
        if (!DPUtil.parseString(param.get("name")).equals(info.getName())) return true;
        if (!DPUtil.parseString(param.get("type")).equals(info.getType())) return true;
        if (!DPUtil.parseString(param.get("content")).equals(info.getContent())) return true;
        if (!DPUtil.implode(",", DPUtil.parseStringList(param.get("partitions"))).equals(info.getPartitions())) return true;
        return false;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "数据集名称不能为空", name);
        if (!ValidateUtil.isSnake(name)) return ApiUtil.result(1009, "名称不合法", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(!types().containsKey(type)) return ApiUtil.result(1002, "服务方式异常", type);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1005, "状态异常", status);
        Dataset info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Dataset();
        }
        int count = datasetDao.exist(name, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1501, "名称已存在", name);
        }
        boolean changed = changed(info, param);
        String oldName = info.getName();
        String oldType = info.getType();
        info.setName(name);
        info.setType(type);
        info.setExpression(DPUtil.parseString(param.get("expression")));
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setPks(DPUtil.implode(",", DPUtil.parseStringList(param.get("pks"))));
        info.setPartitions(DPUtil.implode(",", DPUtil.parseStringList(param.get("partitions"))));
        info.setFields(DPUtil.stringify(param.get("fields")));
        info.setLabels(DPUtil.implode(",", DPUtil.parseStringList(param.get("labels"))));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        if ("cron".equals(type) && DPUtil.empty(info.getExpression())) {
            return ApiUtil.result(1006, "定时同步数据集的表达式不能为空", null);
        }
        if (changed) {
            Map<String, Object> syncViewResult = syncView(info, oldName, oldType);
            if (ApiUtil.failed(syncViewResult)) return syncViewResult;
        }
        Map<String, Object> syncJobResult = syncJob(info);
        if (ApiUtil.failed(syncJobResult)) return syncJobResult;
        info = save(datasetDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(datasetDao, param, (root, query, cb) -> {
            SpecificationHelper<Dataset> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").equal("type");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withRoles"))) {
            rbacService.fillInfos(rows);
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        fillStatus(rows, status());
        DPUtil.fillValues(rows, "type", "typeText", types());
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<String> pks = DPUtil.parseStringList(node.at("/pks").asText(""));
            node.replace("pks", DPUtil.toJSON(pks));
            List<String> partitions = DPUtil.parseStringList(node.at("/partitions").asText(""));
            node.replace("partitions", DPUtil.toJSON(partitions));
            node.replace("fields", DPUtil.parseJSON(node.at("/fields").asText("[]")));
            List<String> labels = DPUtil.parseStringList(node.at("/labels").asText(""));
            node.replace("labels", DPUtil.toJSON(labels));
            List<Integer> roleIds = DPUtil.parseIntList(node.at("/roleIds").asText(""));
            node.replace("roleIds", DPUtil.toJSON(roleIds));
        }
        return rows;
    }

    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, "未指定有效记录", ids);
        List<Dataset> list = datasetDao.findAllById(ids);
        if (list.isEmpty()) return ApiUtil.result(0, "未检索到有效记录", ids);
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (Dataset info : list) {
            try {
                trinoService.dropView(info.getName(), isMaterialized(info.getType()));
            } catch (Exception e) {
                return ApiUtil.result(1501, String.format("删除视图失败, id: %d, message: %s", info.getId(), e.getMessage()), ids);
            }
            Map<String, Object> job = new LinkedHashMap<>();
            job.put("group", DatasetService.class.getName());
            job.put("name", String.valueOf(info.getId()));
            jobs.add(job);
        }
        Map<String, Object> deleteParam = new LinkedHashMap<>();
        deleteParam.put("jobs", jobs);
        Map<String, Object> jobResult = RpcUtil.result(cronRpc.delete(deleteParam));
        if (ApiUtil.failed(jobResult)) {
            return ApiUtil.result(1502, "删除作业失败: " + ApiUtil.message(jobResult), ids);
        }
        boolean removed = remove(datasetDao, ids);
        return ApiUtil.result(0, "已删除" + removed + "条记录", ids);
    }

    public Map<String, Object> trigger(Map<?, ?> param) {
        Integer id = DPUtil.parseInt(param.get("id"));
        Dataset info = info(id);
        if (null == info) return ApiUtil.result(1404, "数据集信息不存在", id);
        if (!"cron".equals(info.getType())) {
            return ApiUtil.result(1001, "仅定时同步数据集支持手动调度", info.getType());
        }
        Map<String, Object> jobParam = jobParam(info);
        return RpcUtil.result(cronRpc.trigger(jobParam));
    }

    public Map<String, Object> refresh(Map<?, ?> param) {
        Integer id = DPUtil.parseInt(param.get("id"));
        Dataset info = info(id);
        if (null == info) return ApiUtil.result(1404, "数据集信息不存在", id);
        if (!"cron".equals(info.getType())) return ApiUtil.result(1001, "仅定时同步数据集支持刷新", info.getType());
        String sql;
        try {
            sql = trinoService.refreshView(info.getName());
            info.setLastSyncedTime(System.currentTimeMillis());
            datasetDao.save(info);
        } catch (Exception e) {
            return ApiUtil.result(1500, "刷新物化视图失败", e.getMessage());
        }
        return ApiUtil.result(0, null, sql);
    }

    public MaintainEmitter reload(MaintainEmitter emitter) {
        if (null == emitter || !emitter.isRunning()) return emitter;
        Boolean locked = redis.opsForValue().setIfAbsent(
                RedisKey.datasetReloadLock(),
                String.valueOf(System.currentTimeMillis()),
                100,
                TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            emitter.error(1502, "数据集正在重建中", null);
            return emitter;
        }
        try {
            Map<String, String> steps = new LinkedHashMap<>();
            steps.put("dropSchema", "删除数据集 Schema");
            steps.put("rebuildViews", "重建数据集视图");
            steps.put("syncJobs", "同步维护定时任务");
            emitter.plan(steps);
            emitter.start("开始重建数据集视图", "dataset");
            List<Dataset> datasets;
            try {
                datasets = datasetDao.findAll((Specification<Dataset>) (root, query, cb) -> cb.equal(root.get("status"), 1), Sort.by("sort", "id"));
                emitter.step("正在删除数据集 Schema", "dropSchema", 10, 1);
                trinoService.dropDatasetSchema();
                trinoService.ensureDatasetSchema();
                emitter.log("数据集 Schema 已删除并重建", "dropSchema", 20, "success");

                int total = datasets.size();
                int success = 0;
                emitter.step("正在重建数据集视图", "rebuildViews", 30, total);
                for (int index = 0; index < total; index++) {
                    Dataset info = datasets.get(index);
                    int percent = 30 + (int) Math.round(65.0 * (index + 1) / Math.max(total, 1));
                    emitter.log("正在创建数据集视图：" + info.getName() + " (" + (index + 1) + "/" + total + ")", "rebuildViews", percent);
                    try {
                        trinoService.createView(info, isMaterialized(info.getType()));
                        success++;
                        emitter.log("数据集视图 " + info.getName() + " 创建成功", "rebuildViews", percent, "success", index + 1, total);
                    } catch (Exception e) {
                        emitter.log("数据集视图 " + info.getName() + " 创建失败：" + e.getMessage(), "rebuildViews", percent, "warning", index + 1, total);
                    }
                }
                emitter.log("数据集视图重建完成：成功 " + success + " 个，失败 " + (total - success) + " 个", "rebuildViews", 95);

                int jobSuccess = 0;
                emitter.step("正在同步维护定时任务", "syncJobs", 96, total);
                for (int index = 0; index < total; index++) {
                    Dataset info = datasets.get(index);
                    int percent = 96 + (int) Math.round(4.0 * (index + 1) / Math.max(total, 1));
                    try {
                        Map<String, Object> syncJobResult = syncJob(info);
                        if (ApiUtil.failed(syncJobResult)) {
                            emitter.log("数据集 " + info.getName() + " 定时任务同步失败：" + ApiUtil.message(syncJobResult), "syncJobs", percent, "warning", index + 1, total);
                        } else {
                            jobSuccess++;
                            emitter.log("数据集 " + info.getName() + " 定时任务同步成功", "syncJobs", percent, "success", index + 1, total);
                        }
                    } catch (Exception e) {
                        emitter.log("数据集 " + info.getName() + " 定时任务同步异常：" + e.getMessage(), "syncJobs", percent, "warning", index + 1, total);
                    }
                }
                emitter.log("定时任务同步完成：成功 " + jobSuccess + " 个，失败 " + (total - jobSuccess) + " 个", "syncJobs", 100);
                ObjectNode result = DPUtil.objectNode();
                result.put("total", total);
                result.put("success", success);
                result.put("jobSuccess", jobSuccess);
                boolean failed = success < total || jobSuccess < total;
                emitter.result(failed ? 1500 : 0, failed ? "部分数据集重建或定时任务同步失败" : "全部成功", result);
                return emitter;
            } catch (Exception e) {
                emitter.error(1500, "重建数据集视图失败：" + e.getMessage(), e.getMessage());
                return emitter;
            }
        } finally {
            redis.delete(RedisKey.datasetReloadLock());
        }
    }

    public Map<String, Object> syncView(Dataset info, String oldName, String oldType) {
        try {
            trinoService.dropView(oldName, isMaterialized(oldType));
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除旧视图失败", e.getMessage());
        }
        try {
            trinoService.createView(info, isMaterialized(info.getType()));
        } catch (Exception e) {
            return ApiUtil.result(1500, "创建数据集视图失败", e.getMessage());
        }
        return ApiUtil.result(0, null, info);
    }

    public Map<String, Object> syncJob(Dataset info) {
        Map<String, Object> param = jobParam(info);
        if ("cron".equals(info.getType())) {
            return RpcUtil.result(cronRpc.sync(param));
        }
        return RpcUtil.result(cronRpc.delete(deleteParam(info)));
    }

    private Map<String, Object> deleteParam(Dataset info) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("group", DatasetService.class.getName());
        job.put("name", String.valueOf(info.getId()));
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("jobs", List.of(job));
        return param;
    }

    private Map<String, Object> jobParam(Dataset info) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("group", DatasetService.class.getName());
        param.put("name", String.valueOf(info.getId()));
        if ("cron".equals(info.getType())) {
            param.put("expression", List.of(info.getExpression()));
        }
        param.put("app", "bi");
        param.put("uri", "/rpc/datasetRefresh");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("id", info.getId());
        args.put("name", info.getName());
        param.put("args", args);
        return param;
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(datasetDao, rows, properties);
    }

    public JsonNode fillInfos(JsonNode rows, String ...properties) {
        return fillInfos(datasetDao, rows, properties);
    }

    @Override
    public JsonNode filter(JsonNode json) {
        return format(json);
    }
}
