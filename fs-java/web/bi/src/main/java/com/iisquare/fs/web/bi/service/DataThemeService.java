package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.bi.dao.DataThemeDao;
import com.iisquare.fs.web.bi.dao.DatasetDao;
import com.iisquare.fs.web.bi.entity.DataTheme;
import com.iisquare.fs.web.bi.entity.Dataset;
import com.iisquare.fs.web.bi.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 数据主题服务：组合多个数据集，并维护数据集字段之间的关联关系。
 */
@Service
public class DataThemeService extends JPAServiceBase {

    @Autowired
    DataThemeDao dataThemeDao;
    @Autowired
    DatasetDao datasetDao;
    @Autowired
    DatasetService datasetService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

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

    public DataTheme info(Integer id) {
        return info(dataThemeDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        DataTheme entity = info(id);
        if (null == entity) return ApiUtil.result(404, null, id);
        JsonNode rows = format(DPUtil.toArrayNode(entity), DPUtil.buildMap(
                "withUserInfo", true,
                "withStatusText", true,
                "withDatasetInfo", true,
                "withRoles", true));
        ObjectNode data = (ObjectNode) DPUtil.firstNode(rows);
        data.replace("content", parseContent(entity.getContent()));
        return ApiUtil.result(0, null, data);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataThemeDao, param, (root, query, cb) -> {
            SpecificationHelper<DataTheme> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result), args);
        return result;
    }

    /**
     * 列表输出不包含完整主题配置，仅提供数据集数量及数据集标识集合。
     */
    public JsonNode format(JsonNode rows, Map<?, ?> args) {
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withRoles"))) {
            rbacService.fillInfos(rows);
        }
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            ObjectNode config = parseContent(node.at("/content").asText("{}"));
            node.remove("content");
            JsonNode datasetIds = config.at("/datasetIds");
            int datasetCount = datasetIds.isArray() ? datasetIds.size() : 0;
            node.put("datasetCount", datasetCount);
            node.replace("datasetIds", datasetIds.isArray() ? (ArrayNode) datasetIds.deepCopy() : node.putArray("datasetIds"));
            List<String> labels = DPUtil.parseStringList(node.at("/labels").asText(""));
            node.replace("labels", DPUtil.toJSON(labels));
            List<Integer> roleIds = DPUtil.parseIntList(node.at("/roleIds").asText(""));
            node.replace("roleIds", DPUtil.toJSON(roleIds));
        }
        if (!DPUtil.empty(args.get("withDatasetInfo"))) {
            datasetService.fillInfos(rows, "datasetIds");
        }
        return rows;
    }

    /**
     * 将主题配置字符串解析为对象，非法或缺失内容时返回空配置。
     */
    public ObjectNode parseContent(String content) {
        if (DPUtil.empty(content)) return emptyConfig();
        JsonNode json = DPUtil.parseJSON(content);
        if (null == json || !json.isObject()) return emptyConfig();
        return (ObjectNode) json;
    }

    private ObjectNode emptyConfig() {
        ObjectNode config = DPUtil.objectNode();
        config.putArray("datasetIds");
        config.putArray("relations");
        return config;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "主题名称不能为空", name);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1005, "状态异常", status);
        Object contentValue = param.get("content");
        String content = contentValue instanceof String ? DPUtil.trim((String) contentValue) : DPUtil.stringify(contentValue);
        if (DPUtil.empty(content)) return ApiUtil.result(1002, "主题配置不能为空", content);
        JsonNode config = DPUtil.parseJSON(content);
        if (null == config || !config.isObject()) return ApiUtil.result(1003, "主题配置格式异常", content);
        String validateMessage = validateContent(config);
        if (null != validateMessage) return ApiUtil.result(1501, validateMessage, config);
        DataTheme info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new DataTheme();
        }
        int count = dataThemeDao.exist(name, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1502, "主题名称已存在", name);
        }
        info.setName(name);
        info.setContent(content);
        info.setLabels(DPUtil.implode(",", DPUtil.parseStringList(param.get("labels"))));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(dataThemeDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    /**
     * 校验主题配置：
     * datasetIds 至少包含一个有效数据集，relations 中引用的数据集与字段必须合法，
     * 两侧字段数量一致，表示数据集之间的外键关联。
     */
    public String validateContent(JsonNode config) {
        JsonNode datasetIdsJson = config.get("datasetIds");
        if (null == datasetIdsJson || !datasetIdsJson.isArray() || datasetIdsJson.isEmpty()) {
            return "请至少选择一个数据集";
        }
        List<Integer> datasetIds = new ArrayList<>();
        Set<Integer> datasetIdSet = new HashSet<>();
        for (JsonNode item : datasetIdsJson) {
            int datasetId = item.asInt();
            if (datasetId < 1) return "数据集引用无效：" + item.asText();
            if (!datasetIdSet.add(datasetId)) return "数据集重复引用：" + datasetId;
            datasetIds.add(datasetId);
        }
        List<Dataset> datasets = datasetDao.findAllById(datasetIds);
        Map<Integer, Dataset> datasetMap = new LinkedHashMap<>();
        for (Dataset dataset : datasets) {
            datasetMap.put(dataset.getId(), dataset);
        }
        for (int datasetId : datasetIds) {
            Dataset dataset = datasetMap.get(datasetId);
            if (null == dataset) return "数据集不存在：" + datasetId;
            if (dataset.getStatus() == null || dataset.getStatus() != 1) {
                return "数据集已停用：" + dataset.getName();
            }
        }
        JsonNode relationsJson = config.get("relations");
        if (null == relationsJson || !relationsJson.isArray()) return "字段关联配置格式异常";
        for (JsonNode relation : relationsJson) {
            if (null == relation || !relation.isObject()) return "字段关联配置格式异常";
            int sourceDatasetId = relation.at("/sourceDatasetId").asInt();
            int targetDatasetId = relation.at("/targetDatasetId").asInt();
            if (sourceDatasetId < 1 || !datasetIdSet.contains(sourceDatasetId)) {
                return "字段关联引用了无效的源数据集：" + sourceDatasetId;
            }
            if (targetDatasetId < 1 || !datasetIdSet.contains(targetDatasetId)) {
                return "字段关联引用了无效的目标数据集：" + targetDatasetId;
            }
            if (sourceDatasetId == targetDatasetId) {
                return "字段关联双方不能为同一数据集：" + sourceDatasetId;
            }
            JsonNode sourceFields = relation.get("sourceFields");
            JsonNode targetFields = relation.get("targetFields");
            if (null == sourceFields || !sourceFields.isArray() || sourceFields.isEmpty()) {
                return "关联[" + sourceDatasetId + "-" + targetDatasetId + "]请选择源字段";
            }
            if (null == targetFields || !targetFields.isArray() || targetFields.isEmpty()) {
                return "关联[" + sourceDatasetId + "-" + targetDatasetId + "]请选择目标字段";
            }
            if (sourceFields.size() != targetFields.size()) {
                return "关联[" + sourceDatasetId + "-" + targetDatasetId + "]两侧字段数量必须一致";
            }
            Dataset sourceDataset = datasetMap.get(sourceDatasetId);
            Dataset targetDataset = datasetMap.get(targetDatasetId);
            for (JsonNode field : sourceFields) {
                String sourceField = DPUtil.trim(field.asText());
                if (DPUtil.empty(sourceField)) return "关联[" + sourceDatasetId + "-" + targetDatasetId + "]源字段不能为空";
                if (!containsField(sourceDataset, sourceField)) {
                    return "数据集[" + sourceDataset.getName() + "]不包含字段：" + sourceField;
                }
            }
            for (JsonNode field : targetFields) {
                String targetField = DPUtil.trim(field.asText());
                if (DPUtil.empty(targetField)) return "关联[" + sourceDatasetId + "-" + targetDatasetId + "]目标字段不能为空";
                if (!containsField(targetDataset, targetField)) {
                    return "数据集[" + targetDataset.getName() + "]不包含字段：" + targetField;
                }
            }
        }
        return null;
    }

    private boolean containsField(Dataset dataset, String fieldName) {
        JsonNode fields = DPUtil.parseJSON(dataset.getFields());
        if (null == fields || !fields.isArray() || fields.isEmpty()) return true; // 未维护字段定义时不做强校验
        for (JsonNode field : fields) {
            if (fieldName.equals(field.at("/name").asText())) return true;
        }
        return false;
    }

    public boolean remove(List<Integer> ids) {
        return remove(dataThemeDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String... properties) {
        return fillInfo(dataThemeDao, rows, properties);
    }

    public JsonNode fillInfos(JsonNode rows, String... properties) {
        return fillInfos(dataThemeDao, rows, properties);
    }

}
