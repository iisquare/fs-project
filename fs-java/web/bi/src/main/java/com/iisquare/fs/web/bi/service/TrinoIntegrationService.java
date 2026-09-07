package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.dao.DataApiDao;
import com.iisquare.fs.web.bi.dao.DataExcelDao;
import com.iisquare.fs.web.bi.datasource.HttpConnector;
import com.iisquare.fs.web.bi.entity.DataApi;
import com.iisquare.fs.web.bi.entity.DataExcel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Service
public class TrinoIntegrationService {

    @Autowired
    DataApiDao dataApiDao;
    @Autowired
    DataExcelDao dataExcelDao;
    @Autowired
    DataExcelService dataExcelService;

    @Value("${fs.bi.trino.integration-key:}")
    String integrationKey;
    @Value("${fs.bi.trino.integration-self:}")
    String integrationSelf;

    public Map<String, Object> check(HttpServletRequest request) {
        String key = request.getHeader("x-api-key");
        if (!integrationKey.equals(key)) {
            return ApiUtil.result(70403, "认证失败", null);
        }
        return ApiUtil.result(0, null, null);
    }

    /**
     * Trino 插件有 metadata-cache-ttl-seconds=60 的元数据缓存
     */
    public Map<String, Object> tables(Map<String, Object> param, HttpServletRequest request) {
        String catalog = DPUtil.parseString(param.get("catalog"));
        Map<String, Object> result = check(request);
        if (ApiUtil.failed(result)) return result;
        ObjectNode data = DPUtil.objectNode();
        data.put("catalog", catalog);
        ArrayNode schemas = data.putArray("schemas");
        List<DataApi> apiList = dataApiDao.findAll((Specification<DataApi>) (root, query, cb) -> {
            return cb.and(
                    cb.equal(root.get("status"), 1)
            );
        });
        List<DataExcel> excelList = dataExcelDao.findAll((Specification<DataExcel>) (root, query, cb) -> {
            return cb.and(
                    cb.equal(root.get("status"), 1)
            );
        });
        schemas.add(schemaNode("api", DPUtil.toJSON(apiList)));
        schemas.add(schemaNode("excel", DPUtil.toJSON(excelList)));
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> data(Map<String, Object> param, HttpServletRequest request) {
        String catalog = DPUtil.parseString(param.get("catalog"));
        String schema = DPUtil.parseString(param.get("schema"));
        String table = DPUtil.parseString(param.get("table"));
        long offset = DPUtil.parseLong(param.get("offset"));
        int limit = DPUtil.parseInt(param.get("limit"));
        Map<String, Object> result = check(request);
        if (ApiUtil.failed(result)) return result;
        if (limit < 1 || limit > 1000) {
            limit = 500;
        }
        if ("excel".equals(schema)) {
            return excelData(table, offset, limit);
        }
        if ("api".equals(schema)) {
            return apiData(table, offset, limit);
        }
        return ApiUtil.result(1002, "未注册的schema", schema);
    }

    private Map<String, Object> excelData(String table, long offset, int limit) {
        DataExcel info = dataExcelDao.findByName(table);
        if (info == null) {
            return ApiUtil.result(1404, "数据表不存在", table);
        }
        int page = (int) (offset / limit) + 1;
        Map<String, Object> result = dataExcelService.listMongoData(
                params("id", info.getId(), "page", page, "pageSize", limit));
        if (ApiUtil.failed(result)) {
            return result;
        }
        JsonNode pageData = ApiUtil.data(result, JsonNode.class);
        JsonNode rawRows = DPUtil.toJSON(pageData.at("/rows"));
        long total = pageData.at("/total").asLong(0);
        List<JsonNode> rows = mapExcelRows(rawRows, DPUtil.parseJSON(info.getFields()));
        return dataPage(rows, total, offset, limit);
    }

    private Map<String, Object> apiData(String table, long offset, int limit) {
        DataApi info = dataApiDao.findByName(table);
        if (info == null) {
            return ApiUtil.result(1404, "数据表不存在", table);
        }
        ObjectNode config = DPUtil.objectNode();
        config.put("url", info.getUrl());
        config.put("method", info.getMethod());
        config.put("timeout", info.getTimeout());
        JsonNode headers = DPUtil.parseJSON(info.getHeaders());
        if (headers == null) {
            config.putNull("headers");
        } else {
            config.set("headers", headers);
        }
        config.put("contentType", info.getContentType());
        JsonNode payloadForm = DPUtil.parseJSON(info.getPayloadForm());
        if (payloadForm == null) {
            config.putNull("payloadForm");
        } else {
            config.set("payloadForm", payloadForm);
        }
        config.put("payloadBody", info.getPayloadBody());
        if (DPUtil.empty(info.getPageRequestField())) {
            config.putNull("pageRequestField");
        } else {
            config.put("pageRequestField", info.getPageRequestField());
        }
        if (DPUtil.empty(info.getPageSizeRequestField())) {
            config.putNull("pageSizeRequestField");
        } else {
            config.put("pageSizeRequestField", info.getPageSizeRequestField());
        }
        if (DPUtil.empty(info.getPageResponseField())) {
            config.putNull("pageResponseField");
        } else {
            config.put("pageResponseField", info.getPageResponseField());
        }
        if (DPUtil.empty(info.getPageSizeResponseField())) {
            config.putNull("pageSizeResponseField");
        } else {
            config.put("pageSizeResponseField", info.getPageSizeResponseField());
        }
        if (DPUtil.empty(info.getTotalResponseField())) {
            config.putNull("totalResponseField");
        } else {
            config.put("totalResponseField", info.getTotalResponseField());
        }
        try {
            HttpConnector connector = new HttpConnector("http", config);
            long page = offset / limit + 1;
            JsonNode response = connector.execute(page, limit);
            JsonNode fields = DPUtil.parseJSON(info.getFields());
            List<JsonNode> rows = extractApiRows(response, fields);
            long total = connector.total(response);
            boolean hasMore = total >= 0
                    ? offset + rows.size() < total
                    : connector.hasMore(response, rows.size(), limit);
            return dataPage(rows, total, offset, limit, hasMore);
        } catch (Exception e) {
            return ApiUtil.result(1500, "接口执行失败 - " + e.getMessage(), info.getUrl());
        }
    }

    private ObjectNode schemaNode(String name, JsonNode rows) {
        ObjectNode schema = DPUtil.objectNode();
        schema.put("name", name);
        ArrayNode tables = schema.putArray("tables");
        if (rows != null && rows.isArray()) {
            for (JsonNode row : rows) {
                String table = row.at("/name").asText("");
                if (table.isEmpty()) {
                    continue;
                }
                ObjectNode tableNode = DPUtil.objectNode();
                tableNode.put("name", table);
                ArrayNode columns = tableNode.putArray("columns");
                JsonNode fields = row.get("fields");
                if (fields != null && fields.isTextual()) {
                    fields = DPUtil.parseJSON(fields.asText());
                }
                if ("api".equals(name)) {
                    appendApiColumns(columns, fields);
                } else {
                    appendExcelColumns(columns, fields);
                }
                tables.add(tableNode);
            }
        }
        return schema;
    }

    private void appendExcelColumns(ArrayNode columns, JsonNode fields) {
        if (fields == null || !fields.isArray()) {
            return;
        }
        for (JsonNode field : fields) {
            String name = field.at("/name").asText("");
            if (name.isEmpty()) {
                continue;
            }
            ObjectNode column = DPUtil.objectNode();
            column.put("name", name);
            column.put("type", trinoType(field.at("/type").asText("string")));
            columns.add(column);
        }
    }

    private void appendApiColumns(ArrayNode columns, JsonNode fields) {
        JsonNode arrayNode = selectArrayNode(fields);
        if (arrayNode == null) {
            return;
        }
        for (JsonNode leaf : selectedLeaves(arrayNode)) {
            ObjectNode column = DPUtil.objectNode();
            column.put("name", apiColumnName(leaf));
            column.put("type", trinoType(leaf.at("/type").asText("string")));
            columns.add(column);
        }
    }

    private List<JsonNode> mapExcelRows(JsonNode rawRows, JsonNode fields) {
        List<String> names = new ArrayList<>();
        if (fields != null && fields.isArray()) {
            for (JsonNode field : fields) {
                String name = field.at("/name").asText("");
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        List<JsonNode> rows = new ArrayList<>();
        if (rawRows == null || !rawRows.isArray()) {
            return rows;
        }
        for (JsonNode raw : rawRows) {
            ObjectNode row = DPUtil.objectNode();
            for (String name : names) {
                JsonNode value = raw.get(name);
                if (value == null || value.isNull()) {
                    row.putNull(name);
                } else {
                    row.set(name, value);
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private List<JsonNode> extractApiRows(JsonNode response, JsonNode fields) {
        List<JsonNode> rows = new ArrayList<>();
        JsonNode arrayNode = selectArrayNode(fields);
        if (arrayNode == null) {
            return rows;
        }
        String arrayPath = arrayNode.at("/path").asText("");
        JsonNode array = resolve(response, arrayPath);
        if (array == null || !array.isArray()) {
            return rows;
        }
        List<JsonNode> leaves = selectedLeaves(arrayNode);
        for (JsonNode raw : array) {
            ObjectNode row = DPUtil.objectNode();
            for (JsonNode leaf : leaves) {
                String name = apiColumnName(leaf);
                String sourcePath = sourcePath(arrayPath, leaf.at("/path").asText(""));
                JsonNode value = sourcePath.isEmpty() ? raw : resolve(raw, sourcePath);
                if (value == null || value.isNull()) {
                    row.putNull(name);
                } else {
                    row.set(name, value);
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private JsonNode selectArrayNode(JsonNode fields) {
        List<JsonNode> arrays = new ArrayList<>();
        collectNodes(fields, arrays);
        arrays.removeIf(node -> !"array".equals(node.at("/type").asText()));
        if (arrays.isEmpty()) {
            return null;
        }
        for (JsonNode array : arrays) {
            if (hasChecked(array)) {
                return array;
            }
        }
        return arrays.stream()
                .min(java.util.Comparator.comparingInt(node -> node.at("/path").asText().split("\\.").length))
                .orElse(null);
    }

    private List<JsonNode> selectedLeaves(JsonNode arrayNode) {
        List<JsonNode> leaves = new ArrayList<>();
        collectLeaves(arrayNode, leaves);
        if (leaves.isEmpty()) {
            leaves.add(arrayNode);
        }
        List<JsonNode> checked = leaves.stream()
                .filter(node -> node.at("/checked").asBoolean(false))
                .toList();
        return checked.isEmpty() ? leaves : checked;
    }

    private static void collectNodes(JsonNode node, List<JsonNode> result) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectNodes(child, result);
            }
            return;
        }
        result.add(node);
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                collectNodes(child, result);
            }
        }
    }

    private static void collectLeaves(JsonNode node, List<JsonNode> result) {
        if (node == null || node.isNull()) {
            return;
        }
        JsonNode children = node.get("children");
        if (children == null || !children.isArray() || children.isEmpty()) {
            result.add(node);
            return;
        }
        for (JsonNode child : children) {
            collectLeaves(child, result);
        }
    }

    private static boolean hasChecked(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.at("/checked").asBoolean(false)) {
            return true;
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                if (hasChecked(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String apiColumnName(JsonNode leaf) {
        String name = leaf.at("/name").asText("");
        if (!name.isEmpty()) {
            return name;
        }
        name = leaf.at("/field").asText("");
        if (!name.isEmpty()) {
            return name;
        }
        String path = leaf.at("/path").asText("");
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }

    private static String sourcePath(String arrayPath, String leafPath) {
        if (arrayPath.isEmpty()) {
            return leafPath;
        }
        if (leafPath.equals(arrayPath)) {
            return "";
        }
        String prefix = arrayPath + ".";
        return leafPath.startsWith(prefix) ? leafPath.substring(prefix.length()) : leafPath;
    }

    private static JsonNode resolve(JsonNode root, String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || current.isNull()) {
                return null;
            }
            if (current.isArray()) {
                try {
                    current = current.get(Integer.parseInt(segment));
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                current = current.get(segment);
            }
        }
        return current;
    }

    private Map<String, Object> dataPage(List<JsonNode> rows, long total, long offset, int limit) {
        boolean hasMore = total >= 0 && offset + rows.size() < total;
        return dataPage(rows, total, offset, limit, hasMore);
    }

    private Map<String, Object> dataPage(List<JsonNode> rows, long total, long offset, int limit, boolean hasMore) {
        ObjectNode data = DPUtil.objectNode();
        ArrayNode array = data.putArray("rows");
        rows.forEach(array::add);
        data.put("total", total >= 0 ? total : offset + rows.size());
        data.put("hasMore", hasMore);
        if (hasMore) {
            data.put("nextOffset", offset + rows.size());
        }
        return ApiUtil.result(0, null, data);
    }

    private Map<String, Object> params(Object... values) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            params.put(String.valueOf(values[i]), values[i + 1]);
        }
        return params;
    }

    private String trinoType(String type) {
        return switch (type) {
            case "boolean" -> "boolean";
            case "integer", "long" -> "bigint";
            case "float", "double" -> "double";
            case "date" -> "date";
            case "time" -> "time";
            case "datetime" -> "timestamp";
            default -> "varchar";
        };
    }
}
