package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.kg.schema.SchemaDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Neo4j 索引与约束管理（历史接口适配）
 *
 * 完整能力由 SchemaService 提供，本服务保留原有接口形态以兼容既有前端调用。
 */
@Service
public class Neo4jService {

    @Autowired
    SchemaService schemaService;

    public Map<String, Object> showIndex(Map<String, Object> param) {
        return rows(kind(param, SchemaDefinition.KIND_INDEX));
    }

    public Map<String, Object> showConstraint(Map<String, Object> param) {
        return rows(kind(param, SchemaDefinition.KIND_CONSTRAINT));
    }

    /**
     * 索引与约束全量查询
     */
    public Map<String, Object> showSchema(Map<String, Object> param) {
        Map<String, Object> indexes = showIndex(param);
        Map<String, Object> constraints = showConstraint(param);
        ObjectNode data = DPUtil.objectNode();
        data.set("indexes", (JsonNode) indexes.get(ApiUtil.FIELD_DATA));
        data.set("constraints", (JsonNode) constraints.get(ApiUtil.FIELD_DATA));
        boolean succeed = ApiUtil.succeed(indexes) && ApiUtil.succeed(constraints);
        return ApiUtil.result(succeed ? 0 : 500, succeed ? null : "查询失败", data);
    }

    public Map<String, Object> createIndex(Map<String, Object> param) {
        return schemaService.create(kind(param, SchemaDefinition.KIND_INDEX));
    }

    public Map<String, Object> dropIndex(Map<String, Object> param) {
        return schemaService.drop(kind(param, SchemaDefinition.KIND_INDEX));
    }

    public Map<String, Object> createConstraint(Map<String, Object> param) {
        return schemaService.create(kind(param, SchemaDefinition.KIND_CONSTRAINT));
    }

    public Map<String, Object> dropConstraint(Map<String, Object> param) {
        return schemaService.drop(kind(param, SchemaDefinition.KIND_CONSTRAINT));
    }

    /**
     * 批量创建索引与约束
     */
    public Map<String, Object> createSchema(Map<String, Object> param) {
        Map<String, Object> batch = new LinkedHashMap<>();
        ArrayNode creates = DPUtil.arrayNode();
        append(creates, DPUtil.toJSON(param.get("indexes")), SchemaDefinition.KIND_INDEX);
        append(creates, DPUtil.toJSON(param.get("constraints")), SchemaDefinition.KIND_CONSTRAINT);
        batch.put("creates", creates);
        batch.put("uid", param.get("uid"));
        return schemaService.batch(batch);
    }

    /**
     * 批量删除索引与约束
     */
    public Map<String, Object> dropSchema(Map<String, Object> param) {
        Map<String, Object> batch = new LinkedHashMap<>();
        ArrayNode drops = DPUtil.arrayNode();
        drops.addAll(dropItems(param.get("indexes"), SchemaDefinition.KIND_INDEX));
        drops.addAll(dropItems(param.get("constraints"), SchemaDefinition.KIND_CONSTRAINT));
        batch.put("drops", drops);
        batch.put("uid", param.get("uid"));
        return schemaService.batch(batch);
    }

    protected ArrayNode dropItems(Object object, String kind) {
        ArrayNode drops = DPUtil.arrayNode();
        JsonNode items = DPUtil.toJSON(object);
        if (!items.isArray()) return drops;
        for (JsonNode item : items) {
            ObjectNode node = DPUtil.objectNode();
            node.put("name", item.isObject() ? item.at("/name").asText("") : item.asText(""));
            node.put("kind", kind);
            drops.add(node);
        }
        return drops;
    }

    protected void append(ArrayNode array, JsonNode items, String kind) {
        if (null == items || !items.isArray()) return;
        for (JsonNode item : items) {
            if (!item.isObject()) continue;
            ObjectNode node = (ObjectNode) item.deepCopy();
            node.put("kind", kind);
            array.add(node);
        }
    }

    protected Map<String, Object> rows(Map<String, Object> param) {
        Map<String, Object> result = schemaService.show(param);
        if (ApiUtil.failed(result)) return result;
        JsonNode data = (JsonNode) result.get(ApiUtil.FIELD_DATA);
        return ApiUtil.result(0, null, data.at("/rows"));
    }

    protected Map<String, Object> kind(Map<String, Object> param, String kind) {
        Map<String, Object> result = new LinkedHashMap<>(param);
        result.put("kind", kind);
        return result;
    }

}
