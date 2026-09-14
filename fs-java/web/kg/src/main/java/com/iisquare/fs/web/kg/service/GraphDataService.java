package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import com.iisquare.fs.web.kg.entity.Ontology;
import com.iisquare.fs.web.kg.ontology.OntologyModel;
import com.iisquare.fs.web.kg.util.ExcelUtil;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 图数据管理服务
 *
 * 以本体定义作为元数据，对图数据库中的实体（节点）、关系、属性进行管理与检索。
 * 本服务中的所有校验均以本体定义为准，索引与约束由 Neo4jService 独立管理。
 */
@Service
public class GraphDataService {

    @Autowired
    protected Driver driver;
    @Autowired
    OntologyService ontologyService;
    @Autowired
    DataLogService dataLogService;

    /**
     * 带超时的读取，避免遍历、聚合等查询在异常数据下长时间占用资源
     */
    protected List<org.neo4j.driver.Record> timedRead(Session session, String cql, Value values, long seconds) {
        return session.executeRead(tx -> tx.run(cql, values).list(),
                TransactionConfig.builder().withTimeout(Duration.ofSeconds(seconds)).build());
    }

    /**
     * 单次读取全部唯一性/键约束，返回"标签|字段"集合，避免逐个实体查询
     */
    protected Set<String> uniqueConstraintKeys() {
        Set<String> keys = new LinkedHashSet<>();
        try (Session session = driver.session()) {
            for (JsonNode row : Neo4jUtil.result2json(session.run("SHOW CONSTRAINTS YIELD *"))) {
                String type = DPUtil.parseString(row.at("/type").asText("")).toUpperCase();
                if (!type.contains("UNIQUE") && !type.contains("KEY")) continue;
                JsonNode properties = row.at("/properties");
                if (null == properties || !properties.isArray() || properties.size() != 1) continue;
                String field = properties.get(0).asText("");
                JsonNode labels = row.at("/labelsOrTypes");
                if (null == labels || !labels.isArray()) continue;
                for (JsonNode label : labels) keys.add(label.asText("") + "|" + field);
            }
        } catch (Exception e) {
            return keys;
        }
        return keys;
    }

    /**
     * 记录数据变更，审计失败不影响业务操作
     */
    protected void audit(Map<String, Object> param, String kind, String label, String action,
                         String targets, Object payload, Map<String, Object> result) {
        dataLogService.append(DPUtil.parseInt(param.get("ontologyId")), kind, label, action, targets,
                null == payload ? "" : DPUtil.stringify(payload), DPUtil.parseInt(param.get("uid")),
                ApiUtil.code(result), ApiUtil.message(result));
    }

    /**
     * 读取本体定义并解析为模型
     */
    protected OntologyModel loadModel(Map<String, Object> param) {
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        if (ontologyId < 1) return null;
        return ontologyService.model(ontologyId);
    }

    protected OntologyModel.Entity loadEntity(Map<String, Object> param, Map<String, Object> error) {
        OntologyModel model = loadModel(param);
        if (null == model) {
            error.putAll(ApiUtil.result(1404, "本体不存在", null));
            return null;
        }
        OntologyModel.Entity entity = model.entity(DPUtil.parseString(param.get("entity")));
        if (null == entity) {
            error.putAll(ApiUtil.result(1404, "实体定义不存在", param.get("entity")));
            return null;
        }
        if (DPUtil.empty(entity.getLabel())) {
            error.putAll(ApiUtil.result(1006, "实体未设置标签，无法管理数据", entity.getCode()));
            return null;
        }
        if (DPUtil.empty(entity.getPrimaryField()) || null == entity.field(entity.getPrimaryField())) {
            error.putAll(ApiUtil.result(1006, "实体未设置有效的主键字段，无法管理数据", entity.getCode()));
            return null;
        }
        return entity;
    }

    protected OntologyModel.Relationship loadRelationship(Map<String, Object> param, Map<String, Object> error) {
        OntologyModel model = loadModel(param);
        if (null == model) {
            error.putAll(ApiUtil.result(1404, "本体不存在", null));
            return null;
        }
        OntologyModel.Relationship relationship = model.relationship(DPUtil.parseString(param.get("relationship")));
        if (null == relationship) {
            error.putAll(ApiUtil.result(1404, "关系定义不存在", param.get("relationship")));
            return null;
        }
        if (DPUtil.empty(relationship.getLabel())) {
            error.putAll(ApiUtil.result(1006, "关系未设置标签，无法管理数据", relationship.getCode()));
            return null;
        }
        if (null == relationship.getSourceEntity() || null == relationship.getTargetEntity()) {
            error.putAll(ApiUtil.result(1006, "关系两端实体定义不完整，无法管理数据", relationship.getCode()));
            return null;
        }
        for (OntologyModel.Entity entity : new OntologyModel.Entity[]{relationship.getSourceEntity(), relationship.getTargetEntity()}) {
            if (DPUtil.empty(entity.getPrimaryField()) || null == entity.field(entity.getPrimaryField())) {
                error.putAll(ApiUtil.result(1006, "关系两端实体未设置有效的主键字段，无法管理数据", relationship.getCode()));
                return null;
            }
        }
        return relationship;
    }

    /**
     * 实体与关系的数据统计
     */
    public Map<String, Object> summary(Map<String, Object> param) {
        OntologyModel model = loadModel(param);
        if (null == model) return ApiUtil.result(1404, "本体不存在", null);
        try (Session session = driver.session()) {
            ArrayNode entities = DPUtil.arrayNode();
            Set<String> uniqueKeys = uniqueConstraintKeys();
            for (OntologyModel.Entity entity : model.getEntities()) {
                ObjectNode node = entity.toJson();
                node.put("count", DPUtil.empty(entity.getLabel()) ? 0L : countNodes(session, entity.getLabel()));
                node.put("pkUnique", uniqueKeys.contains(entity.getLabel() + "|" + entity.getPrimaryField()));
                entities.add(node);
            }
            ArrayNode relationships = DPUtil.arrayNode();
            for (OntologyModel.Relationship relationship : model.getRelationships()) {
                ObjectNode node = relationship.toJson();
                node.put("count", DPUtil.empty(relationship.getLabel()) ? 0L : countRelationships(session, relationship.getLabel()));
                relationships.add(node);
            }
            ObjectNode data = DPUtil.objectNode();
            data.set("entities", entities);
            data.set("relationships", relationships);
            data.set("issues", DPUtil.toJSON(model.getIssues()));
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 实体数据检索
     *
     * param:
     *  ontologyId    本体标识
     *  entity        实体标识（标签、编码或元素标识）
     *  keyword       关键字，匹配字符串类型的字段
     *  filters       过滤条件，支持对象{字段:值}或数组[{field, operator, value}]
     *  sortField     排序字段，默认主键
     *  sortOrder     排序方向，asc/desc
     *  page/pageSize 分页
     */
    public Map<String, Object> search(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        OntologyModel.Entity entity = loadEntity(param, error);
        if (null == entity) return error;
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> where = new ArrayList<>();
        String message = appendFilters("n", entity.getFields(), DPUtil.toJSON(param.get("filters")), where, parameters);
        if (null != message) return ApiUtil.result(1001, message, null);
        message = appendKeyword("n", entity, DPUtil.parseString(param.get("keyword")), where, parameters);
        if (null != message) return ApiUtil.result(1001, message, null);
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = pageSize(param, 15);
        String sortField = DPUtil.parseString(param.get("sortField"));
        if (DPUtil.empty(sortField) || null == entity.field(sortField)) sortField = entity.getPrimaryField();
        String direction = "desc".equalsIgnoreCase(DPUtil.parseString(param.get("sortOrder"))) ? "DESC" : "ASC";
        String condition = where.isEmpty() ? "" : " WHERE " + DPUtil.implode(" AND ", where);
        String requireLabel = DPUtil.parseString(param.get("label"));
        String match = "MATCH (n:" + quote(entity.getLabel())
                + (DPUtil.empty(requireLabel) ? "" : ":" + quote(requireLabel)) + ")" + condition;
        String countCql = match + " RETURN COUNT(n) AS total";
        String dataCql = match + " RETURN n ORDER BY n." + quote(sortField) + " " + direction
                + " SKIP $skip LIMIT $limit";
        parameters.put("skip", (page - 1) * pageSize);
        parameters.put("limit", pageSize);
        Value values = Neo4jUtil.parameters(parameters);
        try (Session session = driver.session()) {
            long total = session.run(countCql, values).single().get("total").asLong();
            ArrayNode rows = DPUtil.arrayNode();
            Result result = session.run(dataCql, values);
            while (result.hasNext()) rows.add(nodeJson(result.next().get("n").asNode()));
            ObjectNode data = DPUtil.objectNode();
            data.put(ApiUtil.FIELD_DATA_PAGE, page)
                    .put(ApiUtil.FIELD_DATA_PAGE_SIZE, pageSize)
                    .put(ApiUtil.FIELD_DATA_TOTAL, total)
                    .set(ApiUtil.FIELD_DATA_ROWS, rows);
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 实体数据详情
     */
    public Map<String, Object> info(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        OntologyModel.Entity entity = loadEntity(param, error);
        if (null == entity) return error;
        String id = DPUtil.parseString(param.get("id"));
        if (DPUtil.empty(id)) return ApiUtil.result(1001, "主键值不能为空", null);
        String cql = "MATCH (n:" + quote(entity.getLabel()) + ") WHERE n." + quote(entity.getPrimaryField()) + " = $id RETURN n";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id", convertValue(entity.field(entity.getPrimaryField()), DPUtil.toJSON(id)));
        Value values = Neo4jUtil.parameters(parameters);
        try (Session session = driver.session()) {
            Result result = session.run(cql, values);
            if (!result.hasNext()) return ApiUtil.result(1404, "数据不存在", null);
            return ApiUtil.result(0, null, nodeJson(result.next().get("n").asNode()));
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 实体数据保存（按主键合并）
     *
     * param:
     *  ontologyId 本体标识
     *  entity     实体标识
     *  properties 属性值，必须包含主键字段
     */
    public Map<String, Object> save(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        OntologyModel.Entity entity = loadEntity(param, error);
        if (null == entity) return error;
        JsonNode properties = DPUtil.toJSON(param.get("properties"));
        if (!properties.isObject() || properties.isEmpty()) return ApiUtil.result(1001, "实体属性不能为空", null);
        String primaryField = entity.getPrimaryField();
        JsonNode primaryValue = properties.get(primaryField);
        if (null == primaryValue || primaryValue.isNull() || DPUtil.empty(primaryValue.asText())) {
            return ApiUtil.result(1001, String.format("主键字段[%s]不能为空", primaryField), null);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            OntologyModel.Field field = entity.field(entry.getKey());
            if (null == field) {
                if (!entity.isExtendable()) {
                    return ApiUtil.result(1002, String.format("属性[%s]未在实体[%s]中定义", entry.getKey(), entity.getLabel()), null);
                }
                values.put(entry.getKey(), convertScalar("", entry.getValue()));
                continue;
            }
            values.put(entry.getKey(), convertValue(field, entry.getValue()));
        }
        for (OntologyModel.Field field : entity.getFields()) {
            if (!field.isRequired()) continue;
            Object value = values.get(field.getName());
            if (null == value || (value instanceof String && DPUtil.empty(value))) {
                return ApiUtil.result(1003, String.format("必填属性[%s]不能为空", field.getName()), null);
            }
        }
        List<String> extraLabels = new ArrayList<>();
        for (String label : entity.getLabels()) {
            if (DPUtil.empty(label) || label.equals(entity.getLabel())) continue;
            extraLabels.add(label);
        }
        List<String> dynamicLabels = new ArrayList<>();
        for (String label : parseIds(param.get("labels"))) {
            if (DPUtil.empty(label) || label.equals(entity.getLabel()) || extraLabels.contains(label)) continue;
            if (!OntologyModel.SAFE_NAME.matcher(label).matches()) {
                return ApiUtil.result(1004, String.format("标签[%s]只能由字母、数字、下划线组成且以字母开头", label), null);
            }
            dynamicLabels.add(label);
        }
        if (!dynamicLabels.isEmpty() && !entity.isExtendableLabels()) {
            return ApiUtil.result(1004, String.format("实体[%s]不允许扩展标签，请先在本体中开启", entity.getLabel()), null);
        }
        List<String> mergeLabels = new ArrayList<>(extraLabels);
        mergeLabels.addAll(dynamicLabels);
        StringBuilder labelClause = new StringBuilder();
        for (String label : mergeLabels) labelClause.append(":").append(quote(label));
        String cql = "MERGE (n:" + quote(entity.getLabel()) + " {" + quote(primaryField) + ": $pk})"
                + (labelClause.length() > 0 ? " SET n" + labelClause + ", n += $values" : " SET n += $values")
                + " RETURN n";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("pk", values.remove(primaryField));
        parameters.put("values", values);
        try (Session session = driver.session()) {
            Result result = session.run(cql, Neo4jUtil.parameters(parameters));
            if (!result.hasNext()) return ApiUtil.result(500, "数据保存失败", null);
            ObjectNode node = nodeJson(result.next().get("n").asNode());
            audit(param, "ENTITY", entity.getLabel(), "SAVE", DPUtil.parseString(parameters.get("pk")), properties,
                    ApiUtil.result(0, null, null));
            return ApiUtil.result(0, null, node);
        } catch (Exception e) {
            audit(param, "ENTITY", entity.getLabel(), "SAVE", DPUtil.parseString(parameters.get("pk")), properties,
                    ApiUtil.result(500, e.getMessage(), null));
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 实体数据删除
     *
     * param:
     *  ontologyId 本体标识
     *  entity     实体标识
     *  ids        主键值列表
     *  detach     是否级联删除关系，默认true
     */
    public Map<String, Object> remove(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        OntologyModel.Entity entity = loadEntity(param, error);
        if (null == entity) return error;
        List<JsonNode> nodes = parseIdNodes(param.get("ids"));
        if (nodes.isEmpty()) return ApiUtil.result(1001, "待删除的主键值不能为空", null);
        OntologyModel.Field primaryField = entity.field(entity.getPrimaryField());
        List<Object> ids = new ArrayList<>();
        for (JsonNode node : nodes) ids.add(convertValue(primaryField, node));
        OntologyModel model = loadModel(param);
        boolean detach;
        if (param.containsKey("detach")) {
            detach = DPUtil.parseBoolean(param.get("detach"));
        } else {
            // 依据本体中的级联策略：存在不允许级联删除的关系时，要求调用方显式确认
            String limited = "";
            for (OntologyModel.Relationship item : model.getRelationships()) {
                boolean related = (null != item.getSourceEntity() && entity.getLabel().equals(item.getSourceEntity().getLabel()))
                        || (null != item.getTargetEntity() && entity.getLabel().equals(item.getTargetEntity().getLabel()));
                if (related && !item.isCascadeDelete()) {
                    limited = DPUtil.empty(limited) ? item.getLabel() : limited + "、" + item.getLabel();
                }
            }
            if (!DPUtil.empty(limited)) {
                return ApiUtil.result(1005, String.format("关系[%s]在本体中配置为不级联删除，如需一并删除关联关系请显式设置detach=true", limited), null);
            }
            detach = true;
        }
        String match = "MATCH (n:" + quote(entity.getLabel()) + ") WHERE n." + quote(entity.getPrimaryField()) + " IN $ids";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("ids", ids);
        Value values = Neo4jUtil.parameters(parameters);
        try (Session session = driver.session()) {
            long count = session.executeWrite(tx -> {
                long total = tx.run(match + " RETURN COUNT(n) AS total", values).single().get("total").asLong();
                if (total > 0) tx.run(match + (detach ? " DETACH DELETE n" : " DELETE n"), values).consume();
                return total;
            });
            audit(param, "ENTITY", entity.getLabel(), "REMOVE", DPUtil.implode(",", ids.toArray(new Object[0])), null,
                    ApiUtil.result(0, null, count));
            return ApiUtil.result(0, null, count);
        } catch (Exception e) {
            audit(param, "ENTITY", entity.getLabel(), "REMOVE", DPUtil.implode(",", ids.toArray(new Object[0])), null,
                    ApiUtil.result(500, e.getMessage(), null));
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 关系数据检索
     *
     * param:
     *  ontologyId   本体标识
     *  relationship 关系标识（标签、编码或元素标识）
     *  source/target 起点/终点实体的主键值
     *  filters      关系属性的过滤条件
     *  page/pageSize 分页
     */
    public Map<String, Object> relationshipSearch(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        OntologyModel.Relationship relationship = loadRelationship(param, error);
        if (null == relationship) return error;
        OntologyModel.Entity sourceEntity = relationship.getSourceEntity();
        OntologyModel.Entity targetEntity = relationship.getTargetEntity();
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> where = new ArrayList<>();
        String source = DPUtil.parseString(param.get("source"));
        if (!DPUtil.empty(source)) {
            where.add("a." + quote(sourceEntity.getPrimaryField()) + " = $source");
            parameters.put("source", convertValue(sourceEntity.field(sourceEntity.getPrimaryField()), DPUtil.toJSON(source)));
        }
        String target = DPUtil.parseString(param.get("target"));
        if (!DPUtil.empty(target)) {
            where.add("b." + quote(targetEntity.getPrimaryField()) + " = $target");
            parameters.put("target", convertValue(targetEntity.field(targetEntity.getPrimaryField()), DPUtil.toJSON(target)));
        }
        String message = appendFilters("r", relationship.getFields(), DPUtil.toJSON(param.get("filters")), where, parameters);
        if (null != message) return ApiUtil.result(1001, message, null);
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = pageSize(param, 15);
        String sortField = DPUtil.parseString(param.get("sortField"));
        if (DPUtil.empty(sortField) || null == relationship.field(sortField)) sortField = "";
        String direction = "desc".equalsIgnoreCase(DPUtil.parseString(param.get("sortOrder"))) ? "DESC" : "ASC";
        String order = DPUtil.empty(sortField)
                ? "a." + quote(sourceEntity.getPrimaryField()) + " " + direction
                : "r." + quote(sortField) + " " + direction;
        String condition = where.isEmpty() ? "" : " WHERE " + DPUtil.implode(" AND ", where);
        String match = "MATCH (a:" + quote(sourceEntity.getLabel()) + ")-[r:" + quote(relationship.getLabel())
                + "]->(b:" + quote(targetEntity.getLabel()) + ")" + condition;
        String countCql = match + " RETURN COUNT(r) AS total";
        String dataCql = match + " RETURN a, r, b ORDER BY " + order + " SKIP $skip LIMIT $limit";
        parameters.put("skip", (page - 1) * pageSize);
        parameters.put("limit", pageSize);
        Value values = Neo4jUtil.parameters(parameters);
        try (Session session = driver.session()) {
            long total = session.run(countCql, values).single().get("total").asLong();
            ArrayNode rows = DPUtil.arrayNode();
            Result result = session.run(dataCql, values);
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                ObjectNode row = DPUtil.objectNode();
                row.set("source", nodeJson(record.get("a").asNode()));
                row.set("relationship", relationshipJson(record.get("r").asRelationship()));
                row.set("target", nodeJson(record.get("b").asNode()));
                rows.add(row);
            }
            ObjectNode data = DPUtil.objectNode();
            data.put(ApiUtil.FIELD_DATA_PAGE, page)
                    .put(ApiUtil.FIELD_DATA_PAGE_SIZE, pageSize)
                    .put(ApiUtil.FIELD_DATA_TOTAL, total)
                    .set(ApiUtil.FIELD_DATA_ROWS, rows);
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 关系数据保存（按两端实体主键合并）
     *
     * param:
     *  ontologyId   本体标识
     *  relationship 关系标识
     *  source/target 起点/终点实体的主键值
     *  properties   关系属性值
     */
    public Map<String, Object> relationshipSave(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        OntologyModel.Relationship relationship = loadRelationship(param, error);
        if (null == relationship) return error;
        OntologyModel.Entity sourceEntity = relationship.getSourceEntity();
        OntologyModel.Entity targetEntity = relationship.getTargetEntity();
        String source = DPUtil.parseString(param.get("source"));
        String target = DPUtil.parseString(param.get("target"));
        if (DPUtil.empty(source) || DPUtil.empty(target)) return ApiUtil.result(1001, "关系两端的实体主键不能为空", null);
        Map<String, Object> values = new LinkedHashMap<>();
        JsonNode properties = DPUtil.toJSON(param.get("properties"));
        if (properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                OntologyModel.Field field = relationship.field(entry.getKey());
                if (null == field) {
                    return ApiUtil.result(1002, String.format("属性[%s]未在关系[%s]中定义", entry.getKey(), relationship.getLabel()), null);
                }
                values.put(entry.getKey(), convertValue(field, entry.getValue()));
            }
        }
        for (OntologyModel.Field field : relationship.getFields()) {
            if (!field.isRequired()) continue;
            Object value = values.get(field.getName());
            if (null == value || (value instanceof String && DPUtil.empty(value))) {
                return ApiUtil.result(1003, String.format("必填属性[%s]不能为空", field.getName()), null);
            }
        }
        String mergeKey = "";
        List<String> mergeFields = relationship.getMergeFields();
        if (null != mergeFields && !mergeFields.isEmpty()) {
            List<String> items = new ArrayList<>();
            for (String field : mergeFields) {
                if (null == relationship.field(field)) {
                    return ApiUtil.result(1002, String.format("关系键字段[%s]未在关系[%s]中定义", field, relationship.getLabel()), null);
                }
                if (!values.containsKey(field) || null == values.get(field)) {
                    return ApiUtil.result(1003, String.format("关系键字段[%s]不能为空", field), null);
                }
                items.add("`" + field + "`: $merge_" + field);
            }
            mergeKey = " {" + DPUtil.implode(", ", items) + "}";
        }
        String cql = "MATCH (a:" + quote(sourceEntity.getLabel()) + " {" + quote(sourceEntity.getPrimaryField()) + ": $source})"
                + ", (b:" + quote(targetEntity.getLabel()) + " {" + quote(targetEntity.getPrimaryField()) + ": $target})"
                + " MERGE (a)-[r:" + quote(relationship.getLabel()) + mergeKey + "]->(b) SET r += $values RETURN a, r, b";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("source", convertValue(sourceEntity.field(sourceEntity.getPrimaryField()), DPUtil.toJSON(source)));
        parameters.put("target", convertValue(targetEntity.field(targetEntity.getPrimaryField()), DPUtil.toJSON(target)));
        parameters.put("values", values);
        if (null != mergeFields) {
            for (String field : mergeFields) parameters.put("merge_" + field, values.get(field));
        }
        try (Session session = driver.session()) {
            Result result = session.run(cql, Neo4jUtil.parameters(parameters));
            if (!result.hasNext()) return ApiUtil.result(1404, "关系两端的实体数据不存在", null);
            org.neo4j.driver.Record record = result.next();
            ObjectNode data = DPUtil.objectNode();
            data.set("source", nodeJson(record.get("a").asNode()));
            data.set("relationship", relationshipJson(record.get("r").asRelationship()));
            data.set("target", nodeJson(record.get("b").asNode()));
            audit(param, "RELATIONSHIP", relationship.getLabel(), "SAVE", source + "->" + target, values,
                    ApiUtil.result(0, null, null));
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            audit(param, "RELATIONSHIP", relationship.getLabel(), "SAVE", source + "->" + target, values,
                    ApiUtil.result(500, e.getMessage(), null));
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 关系数据删除
     *
     * param:
     *  ids 关系元素标识（elementId）列表
     */
    public Map<String, Object> relationshipRemove(Map<String, Object> param) {
        List<String> ids = parseIds(param.get("ids"));
        if (ids.isEmpty()) return ApiUtil.result(1001, "待删除的关系标识不能为空", null);
        String match = "MATCH ()-[r]->() WHERE elementId(r) IN $ids";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("ids", ids);
        Value values = Neo4jUtil.parameters(parameters);
        try (Session session = driver.session()) {
            long count = session.executeWrite(tx -> {
                long total = tx.run(match + " RETURN COUNT(r) AS total", values).single().get("total").asLong();
                if (total > 0) tx.run(match + " DELETE r", values).consume();
                return total;
            });
            audit(param, "RELATIONSHIP", "", "REMOVE", DPUtil.implode(",", ids.toArray(new String[0])), null,
                    ApiUtil.result(0, null, count));
            return ApiUtil.result(0, null, count);
        } catch (Exception e) {
            audit(param, "RELATIONSHIP", "", "REMOVE", DPUtil.implode(",", ids.toArray(new String[0])), null,
                    ApiUtil.result(500, e.getMessage(), null));
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 批量保存数据
     *
     * param:
     *  ontologyId   本体标识
     *  entity       实体标识，批量保存实体时必填
     *  relationship 关系标识，批量保存关系时必填
     *  items        记录列表，实体记录为属性对象，关系记录为{source,target,properties}
     */
    public Map<String, Object> batch(Map<String, Object> param) {
        JsonNode items = DPUtil.toJSON(param.get("items"));
        if (!items.isArray() || items.isEmpty()) return ApiUtil.result(1001, "待保存数据不能为空", null);
        if (items.size() > 50000) return ApiUtil.result(1001, "单次导入不能超过50000条，请分批导入", items.size());
        String relationship = DPUtil.parseString(param.get("relationship"));
        Map<String, Object> error = new LinkedHashMap<>();
        ArrayNode issues = DPUtil.arrayNode();
        if (DPUtil.empty(relationship)) {
            OntologyModel.Entity entity = loadEntity(param, error);
            if (null == entity) return error;
            // 动态标签需要逐条处理，统一走单条逻辑
            boolean dynamicLabels = parseIds(param.get("labels")).size() > 0;
            for (JsonNode item : items) {
                if (item.has("labels")) dynamicLabels = true;
            }
            if (dynamicLabels) return batchByItem(param, items, "ENTITY", entity.getLabel());
            List<Map<String, Object>> records = new ArrayList<>();
            int index = 0;
            for (JsonNode item : items) {
                index++;
                String message = entityValues(entity, item, records);
                if (null != message) issues.add(String.format("第%d条：%s", index, message));
            }
            if (!issues.isEmpty()) return ApiUtil.result(1001, "导入校验未通过，未写入任何数据", issues);
            String cql = batchEntityCql(entity);
            try (Session session = driver.session()) {
                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("items", records);
                long total = session.executeWrite(tx -> tx.run(cql, Neo4jUtil.parameters(parameters))
                        .single().get("total").asLong());
                ObjectNode data = DPUtil.objectNode();
                data.put("total", total);
                audit(param, "ENTITY", entity.getLabel(), "IMPORT", String.format("共%d条", items.size()), null,
                        ApiUtil.result(0, null, null));
                return ApiUtil.result(0, null, data);
            } catch (Exception e) {
                return ApiUtil.result(500, e.getMessage(), issues);
            }
        }
        OntologyModel.Relationship item = loadRelationship(param, error);
        if (null == item) return error;
        if (null != item.getMergeFields() && !item.getMergeFields().isEmpty()) {
            return batchByItem(param, items, "RELATIONSHIP", item.getLabel());
        }
        List<Map<String, Object>> records = new ArrayList<>();
        int index = 0;
        for (JsonNode node : items) {
            index++;
            String source = node.at("/source").asText("");
            String target = node.at("/target").asText("");
            Map<String, Object> values = new LinkedHashMap<>();
            String message = relationshipValues(item, node.at("/properties"), values);
            if (DPUtil.empty(source) || DPUtil.empty(target)) message = "起点与终点不能为空";
            if (null != message) {
                issues.add(String.format("第%d条：%s", index, message));
                continue;
            }
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("source", convertValue(item.getSourceEntity().field(item.getSourceEntity().getPrimaryField()), DPUtil.toJSON(source)));
            record.put("target", convertValue(item.getTargetEntity().field(item.getTargetEntity().getPrimaryField()), DPUtil.toJSON(target)));
            record.put("values", values);
            records.add(record);
        }
        if (!issues.isEmpty()) return ApiUtil.result(1001, "导入校验未通过，未写入任何数据", issues);
        String cql = batchRelationshipCql(item);
        try (Session session = driver.session()) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("items", records);
            long total = session.executeWrite(tx -> tx.run(cql, Neo4jUtil.parameters(parameters))
                    .single().get("total").asLong());
            ObjectNode data = DPUtil.objectNode();
            data.put("total", total);
            data.put("skipped", records.size() - total);
            audit(param, "RELATIONSHIP", item.getLabel(), "IMPORT", String.format("共%d条", items.size()), null,
                    ApiUtil.result(0, null, null));
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), issues);
        }
    }

    /**
     * 动态标签或关系键场景逐条保存
     */
    protected Map<String, Object> batchByItem(Map<String, Object> param, JsonNode items, String kind, String label) {
        ArrayNode results = DPUtil.arrayNode();
        boolean succeed = true;
        for (JsonNode item : items) {
            Map<String, Object> itemParam = new LinkedHashMap<>(param);
            itemParam.remove("items");
            Map<String, Object> result;
            if ("ENTITY".equals(kind)) {
                JsonNode properties = item.deepCopy();
                if (item.has("labels")) itemParam.put("labels", DPUtil.toJSON(item.at("/labels"), List.class));
                if (properties.isObject()) ((ObjectNode) properties).remove("labels");
                itemParam.put("properties", DPUtil.toJSON(properties, Map.class));
                result = save(itemParam);
            } else {
                itemParam.put("source", item.at("/source").asText(""));
                itemParam.put("target", item.at("/target").asText(""));
                itemParam.put("properties", DPUtil.toJSON(item.at("/properties"), Map.class));
                result = relationshipSave(itemParam);
            }
            succeed = appendResult(results, result) && succeed;
        }
        return ApiUtil.result(succeed ? 0 : 500, succeed ? null : "部分数据保存失败", results);
    }

    protected boolean appendResult(ArrayNode results, Map<String, Object> result) {
        ObjectNode node = DPUtil.objectNode();
        node.put("code", ApiUtil.code(result));
        node.put("message", ApiUtil.message(result));
        Object data = result.get(ApiUtil.FIELD_DATA);
        // data可能是字符串等非JSON对象，统一转换，避免强制转换异常
        if (null == data) {
            node.putNull("data");
        } else {
            node.set("data", DPUtil.toJSON(data));
        }
        results.add(node);
        return ApiUtil.succeed(result);
    }

    /**
     * 校验并转换单条实体数据，返回错误信息，校验通过时写入records
     */
    protected String entityValues(OntologyModel.Entity entity, JsonNode properties,
                                  List<Map<String, Object>> records) {
        if (!properties.isObject() || properties.isEmpty()) return "数据不能为空";
        String primaryField = entity.getPrimaryField();
        JsonNode primaryValue = properties.get(primaryField);
        if (null == primaryValue || primaryValue.isNull() || DPUtil.empty(primaryValue.asText())) {
            return String.format("主键字段[%s]不能为空", primaryField);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            OntologyModel.Field field = entity.field(entry.getKey());
            if (null == field) {
                if (!entity.isExtendable()) return String.format("属性[%s]未在实体[%s]中定义", entry.getKey(), entity.getLabel());
                values.put(entry.getKey(), convertScalar("", entry.getValue()));
                continue;
            }
            values.put(entry.getKey(), convertValue(field, entry.getValue()));
        }
        for (OntologyModel.Field field : entity.getFields()) {
            if (!field.isRequired()) continue;
            Object value = values.get(field.getName());
            if (null == value || (value instanceof String && DPUtil.empty(value))) {
                return String.format("必填属性[%s]不能为空", field.getName());
            }
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("pk", values.remove(primaryField));
        record.put("values", values);
        records.add(record);
        return null;
    }

    /**
     * 校验并转换单条关系属性，返回错误信息
     */
    protected String relationshipValues(OntologyModel.Relationship relationship, JsonNode properties, Map<String, Object> values) {
        if (properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                OntologyModel.Field field = relationship.field(entry.getKey());
                if (null == field) return String.format("属性[%s]未在关系[%s]中定义", entry.getKey(), relationship.getLabel());
                values.put(entry.getKey(), convertValue(field, entry.getValue()));
            }
        }
        for (OntologyModel.Field field : relationship.getFields()) {
            if (!field.isRequired()) continue;
            Object value = values.get(field.getName());
            if (null == value || (value instanceof String && DPUtil.empty(value))) {
                return String.format("必填属性[%s]不能为空", field.getName());
            }
        }
        return null;
    }

    /**
     * 单事务批量写入实体
     */
    protected String batchEntityCql(OntologyModel.Entity entity) {
        StringBuilder sb = new StringBuilder("UNWIND $items AS item MERGE (n:");
        sb.append(quote(entity.getLabel())).append(" {").append(quote(entity.getPrimaryField())).append(": item.pk})");
        List<String> extraLabels = new ArrayList<>();
        for (String label : entity.getLabels()) {
            if (DPUtil.empty(label) || label.equals(entity.getLabel())) continue;
            extraLabels.add(label);
        }
        sb.append(" SET n += item.values");
        for (String label : extraLabels) sb.append(", n:").append(quote(label));
        return sb.append(" RETURN COUNT(n) AS total").toString();
    }

    /**
     * 单事务批量写入关系
     */
    protected String batchRelationshipCql(OntologyModel.Relationship relationship) {
        OntologyModel.Entity source = relationship.getSourceEntity();
        OntologyModel.Entity target = relationship.getTargetEntity();
        StringBuilder sb = new StringBuilder("UNWIND $items AS item MATCH (a:");
        sb.append(quote(source.getLabel())).append(" {").append(quote(source.getPrimaryField())).append(": item.source})");
        sb.append(", (b:").append(quote(target.getLabel())).append(" {").append(quote(target.getPrimaryField())).append(": item.target})");
        sb.append(" MERGE (a)-[r:").append(quote(relationship.getLabel())).append("]->(b)");
        sb.append(" SET r += item.values");
        return sb.append(" RETURN COUNT(r) AS total").toString();
    }

    /**
     * 数据导出
     *
     * param: 与检索一致，另支持limit控制导出行数，默认10000，最大50000
     */
    public Map<String, Object> export(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        boolean relationshipMode = !DPUtil.empty(DPUtil.parseString(param.get("relationship")));
        Map<String, Object> query = new LinkedHashMap<>(param);
        query.put("page", 1);
        query.put("pageSize", ValidateUtil.filterInteger(param.get("limit"), 1, 50000, 10000));
        query.put("__export", true);
        ArrayNode fields = DPUtil.arrayNode();
        Map<String, Object> result;
        OntologyModel.Entity entity = null;
        OntologyModel.Relationship relationship = null;
        if (relationshipMode) {
            relationship = loadRelationship(param, error);
            if (null == relationship) return error;
            fields.add(field("source", "起点", "String"));
            for (OntologyModel.Field item : relationship.getFields()) {
                fields.add(field(item.getName(), DPUtil.empty(item.getTitle()) ? item.getName() : item.getTitle(), item.getType()));
            }
            fields.add(field("target", "终点", "String"));
            result = relationshipSearch(query);
        } else {
            entity = loadEntity(param, error);
            if (null == entity) return error;
            if (entity.getFields().isEmpty()) {
                fields.add(field(entity.getPrimaryField(), entity.getPrimaryField(), ""));
            } else {
                for (OntologyModel.Field item : entity.getFields()) {
                    fields.add(field(item.getName(), DPUtil.empty(item.getTitle()) ? item.getName() : item.getTitle(), item.getType()));
                }
            }
            result = search(query);
        }
        if (ApiUtil.failed(result)) return result;
        JsonNode data = (JsonNode) result.get(ApiUtil.FIELD_DATA);
        ArrayNode rows = DPUtil.arrayNode();
        for (JsonNode item : data.at("/rows")) {
            ObjectNode record = DPUtil.objectNode();
            if (relationshipMode) {
                record.put("source", caption(item.at("/source"), relationship.getSourceEntity()));
                JsonNode properties = item.at("/relationship/properties");
                for (OntologyModel.Field field : relationship.getFields()) {
                    JsonNode value = properties.get(field.getName());
                    record.set(field.getName(), null == value ? DPUtil.toJSON(null) : value);
                }
                record.put("target", caption(item.at("/target"), relationship.getTargetEntity()));
            } else {
                JsonNode properties = item.at("/properties");
                for (OntologyModel.Field field : entity.getFields()) {
                    JsonNode value = properties.get(field.getName());
                    record.set(field.getName(), null == value ? DPUtil.toJSON(null) : value);
                }
            }
            rows.add(record);
        }
        ObjectNode output = DPUtil.objectNode();
        output.set("fields", fields);
        output.set("rows", rows);
        output.put("total", data.at("/total").asLong());
        return ApiUtil.result(0, null, output);
    }

    protected ObjectNode field(String name, String title, String type) {
        ObjectNode node = DPUtil.objectNode();
        node.put("name", name);
        node.put("title", title);
        node.put("type", type);
        return node;
    }

    /**
     * 导出为Excel(xlsx)，内容以Base64返回，避免二进制响应与前端下载方式耦合
     */
    public Map<String, Object> exportExcel(Map<String, Object> param) {
        Map<String, Object> result = export(param);
        if (ApiUtil.failed(result)) return result;
        JsonNode data = (JsonNode) result.get(ApiUtil.FIELD_DATA);
        JsonNode fields = data.at("/fields");
        JsonNode rows = data.at("/rows");
        List<String> headers = new ArrayList<>();
        for (JsonNode field : fields) headers.add(field.at("/title").asText(""));
        List<List<Object>> values = new ArrayList<>();
        for (JsonNode row : rows) {
            List<Object> line = new ArrayList<>();
            for (JsonNode field : fields) {
                JsonNode value = row.get(field.at("/name").asText(""));
                if (null == value || value.isNull()) {
                    line.add("");
                } else if (value.isArray()) {
                    List<String> items = new ArrayList<>();
                    for (JsonNode item : value) items.add(item.asText(""));
                    line.add(DPUtil.implode(",", items.toArray(new String[0])));
                } else {
                    line.add(value.asText(""));
                }
            }
            values.add(line);
        }
        try {
            ObjectNode output = DPUtil.objectNode();
            output.put("content", ExcelUtil.write("data", headers, values));
            output.put("total", values.size());
            return ApiUtil.result(0, null, output);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    public static final List<String> NOTICE_HEADERS = Arrays.asList(
            "结构", "字段名", "显示名", "类型", "是否必填", "示例值", "说明");

    /**
     * 导入模板
     *
     * 按本体定义生成节点（实体）与关系模板：数据工作表首行为表头，第二行为可直接改写的示例行。
     * param:
     *  ontologyId   本体标识
     *  entity       实体标识，生成单个实体模板时使用
     *  relationship 关系标识，生成单个关系模板时使用
     *  scope        current-当前结构（默认），all-本体内全部结构（多工作表，仅Excel）
     *  format       xlsx-Excel（默认），csv-逗号分隔文本
     */
    public Map<String, Object> importTemplate(Map<String, Object> param) {
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        OntologyModel model = loadModel(param);
        if (null == model) return ApiUtil.result(1404, "本体不存在", null);
        String format = DPUtil.parseString(param.get("format")).toLowerCase();
        if (DPUtil.empty(format)) format = "xlsx";
        if (!"xlsx".equals(format) && !"csv".equals(format)) {
            return ApiUtil.result(1001, "模板格式仅支持xlsx与csv", format);
        }
        boolean all = "all".equalsIgnoreCase(DPUtil.parseString(param.get("scope")));
        List<ExcelUtil.SheetData> sheets = new ArrayList<>();
        List<List<Object>> notices = new ArrayList<>();
        notices.add(noticeRow("填写须知", "表头请勿修改，示例行可直接改写或删除；单元格留空表示不修改该属性"));
        String title;
        if (all) {
            if (!"xlsx".equals(format)) return ApiUtil.result(1001, "CSV不支持多工作表，请改为Excel模板或选择单个实体/关系", null);
            for (OntologyModel.Entity entity : model.getEntities()) {
                if (DPUtil.empty(entity.getLabel())) continue;
                if (entity.getFields().isEmpty()) {
                    notices.add(noticeRow(structureTitle(entity.getName(), entity.getLabel()), "该实体未定义字段，已跳过"));
                    continue;
                }
                sheets.add(entitySheet(entity));
                coverEntity(notices, entity);
            }
            for (OntologyModel.Relationship relationship : model.getRelationships()) {
                if (DPUtil.empty(relationship.getLabel())) continue;
                if (!relationReady(relationship)) {
                    notices.add(noticeRow(structureTitle(relationship.getName(), relationship.getLabel()), "关系两端定义不完整，已跳过"));
                    continue;
                }
                sheets.add(relationshipSheet(relationship));
                coverRelationship(notices, relationship);
            }
            if (sheets.isEmpty()) return ApiUtil.result(1001, "本体中还没有可用于导入的实体或关系定义", null);
            title = "图数据导入模板";
        } else {
            String relationshipKey = DPUtil.parseString(param.get("relationship"));
            if (!DPUtil.empty(relationshipKey)) {
                OntologyModel.Relationship relationship = model.relationship(relationshipKey);
                if (null == relationship) return ApiUtil.result(1404, "关系定义不存在", relationshipKey);
                if (DPUtil.empty(relationship.getLabel())) return ApiUtil.result(1006, "关系未设置标签，无法生成模板", relationship.getCode());
                if (!relationReady(relationship)) return ApiUtil.result(1006, "关系两端实体定义不完整，无法生成模板", relationship.getCode());
                sheets.add(relationshipSheet(relationship));
                coverRelationship(notices, relationship);
                title = structureTitle(relationship.getName(), relationship.getLabel());
            } else {
                String entityKey = DPUtil.parseString(param.get("entity"));
                OntologyModel.Entity entity = model.entity(entityKey);
                if (null == entity && DPUtil.empty(entityKey) && !model.getEntities().isEmpty()) entity = model.getEntities().get(0);
                if (null == entity) return ApiUtil.result(1404, "实体定义不存在", entityKey);
                if (DPUtil.empty(entity.getLabel())) return ApiUtil.result(1006, "实体未设置标签，无法生成模板", entity.getCode());
                if (entity.getFields().isEmpty()) return ApiUtil.result(1006, "实体未定义字段，无法生成模板", entity.getCode());
                sheets.add(entitySheet(entity));
                coverEntity(notices, entity);
                title = structureTitle(entity.getName(), entity.getLabel());
            }
        }
        Ontology ontology = ontologyService.info(ontologyId);
        String prefix = null == ontology || DPUtil.empty(ontology.getName()) ? "" : ontology.getName() + "-";
        if (!"xlsx".equals(format)) {
            if (1 != sheets.size()) return ApiUtil.result(1001, "CSV不支持多工作表，请改为Excel模板或选择单个实体/关系", null);
            ExcelUtil.SheetData sheet = sheets.get(0);
            ObjectNode output = DPUtil.objectNode();
            output.put("format", format);
            output.put("filename", prefix + sheet.name + "-导入模板.csv");
            output.put("content", csvText(sheet.headers, sheet.rows));
            output.put("sheets", 1);
            return ApiUtil.result(0, null, output);
        }
        sheets.add(1, new ExcelUtil.SheetData("字段说明", NOTICE_HEADERS, notices));
        try {
            ObjectNode output = DPUtil.objectNode();
            output.put("format", format);
            output.put("filename", prefix + title + "-"
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx");
            output.put("content", ExcelUtil.write(sheets));
            output.put("sheets", sheets.size());
            return ApiUtil.result(0, null, output);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    protected List<Object> noticeRow(String structure, String note) {
        List<Object> row = new ArrayList<>();
        row.add(structure);
        row.add("");
        row.add("");
        row.add("");
        row.add("");
        row.add("");
        row.add(note);
        return row;
    }

    /**
     * 实体字段说明：字段名、类型、必填、示例值与注意事项
     */
    protected void coverEntity(List<List<Object>> notices, OntologyModel.Entity entity) {
        String structure = "实体：" + structureTitle(entity.getName(), entity.getLabel());
        for (OntologyModel.Field field : entity.getFields()) {
            boolean primary = field.getName().equals(entity.getPrimaryField());
            List<Object> row = new ArrayList<>();
            row.add(structure);
            row.add(field.getName());
            row.add(field.getTitle());
            row.add(DPUtil.empty(field.getType()) ? "String" : field.getType());
            row.add(field.isRequired() ? "是" : "否");
            row.add(sampleValue(field, primary));
            row.add(fieldNote(field, primary));
            notices.add(row);
        }
        if (entity.isExtendableLabels()) {
            notices.add(noticeRow(structure, "支持扩展标签：可在数据工作表增加 labels 列，多个标签用英文逗号分隔，例如 Dept,Team"));
        }
    }

    /**
     * 关系字段说明：source、target 填写两端实体的主键值
     */
    protected void coverRelationship(List<List<Object>> notices, OntologyModel.Relationship relationship) {
        String structure = "关系：" + structureTitle(relationship.getName(), relationship.getLabel());
        notices.add(noticeRow(structure, "source、target 分别填写两端实体的主键值，两端数据不存在时该条会导入失败"));
        notices.add(noticeRow(structure, "起点实体：" + structureTitle(relationship.getSourceEntity().getName(),
                relationship.getSourceEntity().getLabel()) + "，终点实体：" + structureTitle(relationship.getTargetEntity().getName(),
                relationship.getTargetEntity().getLabel())));
        for (OntologyModel.Field field : relationship.getFields()) {
            List<Object> row = new ArrayList<>();
            row.add(structure);
            row.add(field.getName());
            row.add(field.getTitle());
            row.add(DPUtil.empty(field.getType()) ? "String" : field.getType());
            row.add(field.isRequired() ? "是" : "否");
            row.add(sampleValue(field, false));
            row.add(fieldNote(field, false));
            notices.add(row);
        }
        if (null != relationship.getMergeFields() && !relationship.getMergeFields().isEmpty()) {
            notices.add(noticeRow(structure, "关系键：" + DPUtil.implode(",",
                    relationship.getMergeFields().toArray(new String[0])) + "，同一对实体之间按关系键区分多条关系"));
        }
    }

    protected String fieldNote(OntologyModel.Field field, boolean primary) {
        List<String> parts = new ArrayList<>();
        if (primary) parts.add("主键字段，必填且作为合并依据");
        String type = DPUtil.parseString(field.getType()).toLowerCase();
        if (type.contains("list") || type.contains("array")) parts.add("多个值用英文逗号分隔");
        if (type.contains("bool")) parts.add("填写 true 或 false");
        if (type.contains("date") || type.contains("time")) parts.add("格式 yyyy-MM-dd HH:mm:ss");
        if (!DPUtil.empty(field.getComment())) parts.add(field.getComment());
        return DPUtil.implode("；", parts.toArray(new String[0]));
    }

    protected Object sampleValue(OntologyModel.Field field, boolean primary) {
        String type = DPUtil.parseString(field.getType()).toLowerCase();
        if (type.contains("bool")) return true;
        if (type.contains("int") || type.contains("long")) return 1;
        if (type.contains("float") || type.contains("double") || type.contains("number")) return 1.0;
        if (type.contains("date") || type.contains("time")) return "2025-01-01 00:00:00";
        if (type.contains("list") || type.contains("array")) return "值1,值2";
        if (primary) return "ID001";
        return "示例" + (DPUtil.empty(field.getTitle()) ? field.getName() : DPUtil.trim(field.getTitle()));
    }

    protected ExcelUtil.SheetData entitySheet(OntologyModel.Entity entity) {
        List<String> headers = fieldHeaders(entity.getFields());
        List<Object> sample = new ArrayList<>();
        for (OntologyModel.Field field : entity.getFields()) {
            sample.add(sampleValue(field, field.getName().equals(entity.getPrimaryField())));
        }
        if (entity.isExtendableLabels()) {
            headers.add("labels");
            sample.add("");
        }
        List<List<Object>> rows = new ArrayList<>();
        rows.add(sample);
        return new ExcelUtil.SheetData(structureTitle(entity.getName(), entity.getLabel()), headers, rows);
    }

    protected ExcelUtil.SheetData relationshipSheet(OntologyModel.Relationship relationship) {
        List<String> headers = new ArrayList<>();
        headers.add("source");
        headers.add("target");
        headers.addAll(fieldHeaders(relationship.getFields()));
        List<Object> sample = new ArrayList<>();
        sample.add("ID001");
        sample.add("ID002");
        for (OntologyModel.Field field : relationship.getFields()) sample.add(sampleValue(field, false));
        List<List<Object>> rows = new ArrayList<>();
        rows.add(sample);
        return new ExcelUtil.SheetData(structureTitle(relationship.getName(), relationship.getLabel()), headers, rows);
    }

    /**
     * 表头优先使用显示名称，显示名称缺失、重复或与其它字段名冲突时退回字段名
     */
    protected List<String> fieldHeaders(List<OntologyModel.Field> fields) {
        Set<String> names = new LinkedHashSet<>();
        for (OntologyModel.Field field : fields) names.add(field.getName());
        Set<String> titles = new LinkedHashSet<>();
        boolean titled = true;
        for (OntologyModel.Field field : fields) {
            String title = DPUtil.trim(field.getTitle());
            if (DPUtil.empty(title) || names.contains(title) || !titles.add(title)) {
                titled = false;
                break;
            }
        }
        List<String> headers = new ArrayList<>();
        for (OntologyModel.Field field : fields) {
            headers.add(titled ? DPUtil.trim(field.getTitle()) : field.getName());
        }
        return headers;
    }

    protected boolean relationReady(OntologyModel.Relationship relationship) {
        if (null == relationship.getSourceEntity() || null == relationship.getTargetEntity()) return false;
        OntologyModel.Entity source = relationship.getSourceEntity();
        OntologyModel.Entity target = relationship.getTargetEntity();
        return !DPUtil.empty(source.getPrimaryField()) && null != source.field(source.getPrimaryField())
                && !DPUtil.empty(target.getPrimaryField()) && null != target.field(target.getPrimaryField());
    }

    protected String structureTitle(String name, String label) {
        if (DPUtil.empty(name)) return label;
        if (DPUtil.empty(label) || name.equals(label)) return name;
        return name + "(" + label + ")";
    }

    protected String csvText(List<String> headers, List<List<Object>> rows) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < headers.size(); index++) {
            if (index > 0) builder.append(",");
            builder.append(csvCell(headers.get(index)));
        }
        for (List<Object> row : rows) {
            builder.append("\r\n");
            for (int index = 0; index < row.size(); index++) {
                if (index > 0) builder.append(",");
                builder.append(csvCell(row.get(index)));
            }
        }
        return builder.toString();
    }

    protected String csvCell(Object value) {
        String text = null == value ? "" : DPUtil.parseString(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /**
     * 取节点展示标题，优先标题字段，其次主键字段
     */
    protected String caption(JsonNode node, OntologyModel.Entity entity) {
        JsonNode properties = node.at("/properties");
        if (null != entity) {
            for (String key : new String[]{entity.getCaptionField(), entity.getPrimaryField()}) {
                if (DPUtil.empty(key)) continue;
                String value = properties.at("/" + key).asText("");
                if (!DPUtil.empty(value)) return value;
            }
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
        while (iterator.hasNext()) {
            JsonNode value = iterator.next().getValue();
            if (!value.isNull() && !DPUtil.empty(value.asText())) return value.asText();
        }
        return node.at("/elementId").asText("");
    }

    /**
     * 图数据检索（以某个实体数据为起点进行路径遍历）
     *
     * param:
     *  ontologyId    本体标识
     *  entity        起点实体标识
     *  id            起点实体主键值
     *  direction     方向 out/in/both，默认out
     *  relationships 关系类型列表，为空表示任意关系
     *  depth         遍历深度，1-5，默认1
     *  limit         最大路径数，默认200
     */
    public Map<String, Object> traverse(Map<String, Object> param) {
        OntologyModel model = loadModel(param);
        if (null == model) return ApiUtil.result(1404, "本体不存在", null);
        OntologyModel.Entity entity = model.entity(DPUtil.parseString(param.get("entity")));
        if (null == entity) return ApiUtil.result(1404, "实体定义不存在", param.get("entity"));
        if (DPUtil.empty(entity.getLabel())) return ApiUtil.result(1006, "实体未设置标签，无法检索数据", entity.getCode());
        String id = DPUtil.parseString(param.get("id"));
        if (DPUtil.empty(id)) return ApiUtil.result(1001, "起点实体的主键值不能为空", null);
        List<String> types = new ArrayList<>();
        for (String key : parseIds(param.get("relationships"))) {
            OntologyModel.Relationship relationship = model.relationship(key);
            if (null == relationship) return ApiUtil.result(1002, String.format("关系[%s]未在本体定义中声明", key), null);
            types.add(relationship.getLabel());
        }
        int depth = ValidateUtil.filterInteger(param.get("depth"), 1, 5, 1);
        int limit = ValidateUtil.filterInteger(param.get("limit"), 1, 1000, 200);
        StringBuilder pattern = new StringBuilder("r");
        if (!types.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (String type : types) labels.add(quote(type));
            pattern.append(":").append(DPUtil.implode("|", labels));
        }
        pattern.append("*1..").append(depth);
        String direction = DPUtil.parseString(param.get("direction")).toLowerCase();
        String left = "-", right = "->";
        if ("in".equals(direction)) {
            left = "<-";
            right = "-";
        } else if ("both".equals(direction)) {
            right = "-";
        }
        String cql = "MATCH p = (a:" + quote(entity.getLabel()) + ")" + left + "[" + pattern + "]" + right
                + "(b) WHERE a." + quote(entity.getPrimaryField()) + " = $id RETURN p LIMIT $limit";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("id", convertValue(entity.field(entity.getPrimaryField()), DPUtil.toJSON(id)));
        parameters.put("limit", limit);
        Value values = Neo4jUtil.parameters(parameters);
        try (Session session = driver.session()) {
            ObjectNode nodes = DPUtil.objectNode();
            ObjectNode relationships = DPUtil.objectNode();
            ArrayNode paths = DPUtil.arrayNode();
            for (org.neo4j.driver.Record record : timedRead(session, cql, values, 30)) {
                Path path = record.get("p").asPath();
                ArrayNode nodeIds = DPUtil.arrayNode();
                for (Node node : path.nodes()) {
                    nodes.set(node.elementId(), nodeJson(node));
                    nodeIds.add(node.elementId());
                }
                ArrayNode relationshipIds = DPUtil.arrayNode();
                for (Relationship relationship : path.relationships()) {
                    relationships.set(relationship.elementId(), relationshipJson(relationship));
                    relationshipIds.add(relationship.elementId());
                }
                ObjectNode item = DPUtil.objectNode();
                item.set("nodes", nodeIds);
                item.set("relationships", relationshipIds);
                item.put("length", path.length());
                paths.add(item);
            }
            ObjectNode data = DPUtil.objectNode();
            ArrayNode nodeArray = DPUtil.arrayNode();
            nodes.elements().forEachRemaining(nodeArray::add);
            ArrayNode relationshipArray = DPUtil.arrayNode();
            relationships.elements().forEachRemaining(relationshipArray::add);
            data.set("nodes", nodeArray);
            data.set("relationships", relationshipArray);
            data.set("paths", paths);
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 按字段聚合统计
     *
     * param: ontologyId、entity或relationship、field、filters、keyword、limit
     */
    public Map<String, Object> aggregate(Map<String, Object> param) {
        Map<String, Object> error = new LinkedHashMap<>();
        boolean relationshipMode = !DPUtil.empty(DPUtil.parseString(param.get("relationship")));
        String variable = relationshipMode ? "r" : "n";
        List<OntologyModel.Field> fields;
        String match;
        OntologyModel.Entity entity = null;
        if (relationshipMode) {
            OntologyModel.Relationship relationship = loadRelationship(param, error);
            if (null == relationship) return error;
            fields = relationship.getFields();
            match = "MATCH ()-[r:" + quote(relationship.getLabel()) + "]->()";
        } else {
            entity = loadEntity(param, error);
            if (null == entity) return error;
            fields = entity.getFields();
            match = "MATCH (n:" + quote(entity.getLabel()) + ")";
        }
        String field = DPUtil.parseString(param.get("field"));
        if (field(fields, field) == null) return ApiUtil.result(1001, String.format("字段[%s]未在本体定义中声明", field), null);
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> where = new ArrayList<>();
        String message = appendFilters(variable, fields, DPUtil.toJSON(param.get("filters")), where, parameters);
        if (null != message) return ApiUtil.result(1001, message, null);
        if (!relationshipMode) {
            message = appendKeyword("n", entity, DPUtil.parseString(param.get("keyword")), where, parameters);
            if (null != message) return ApiUtil.result(1001, message, null);
        }
        int limit = ValidateUtil.filterInteger(param.get("limit"), 1, 200, 20);
        String cql = match + " WHERE " + DPUtil.implode(" AND ", where.toArray(new String[0]))
                + " RETURN " + variable + "." + quote(field) + " AS value, COUNT(*) AS total"
                + " ORDER BY total DESC LIMIT " + limit;
        try (Session session = driver.session()) {
            ArrayNode rows = DPUtil.arrayNode();
            for (org.neo4j.driver.Record record : timedRead(session, cql, Neo4jUtil.parameters(parameters), 30)) {
                rows.add(Neo4jUtil.record2json(record));
            }
            return ApiUtil.result(0, null, rows);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 最短路径检索
     *
     * param: ontologyId、from{entity,id}、to{entity,id}、relationships、maxDepth
     */
    public Map<String, Object> shortestPath(Map<String, Object> param) {
        OntologyModel model = loadModel(param);
        if (null == model) return ApiUtil.result(1404, "本体不存在", null);
        String fromEntity = DPUtil.parseString(param.get("fromEntity"));
        String toEntity = DPUtil.parseString(param.get("toEntity"));
        OntologyModel.Entity source = model.entity(fromEntity);
        OntologyModel.Entity target = model.entity(toEntity);
        if (null == source || null == target) return ApiUtil.result(1404, "起点或终点实体定义不存在", null);
        String from = DPUtil.parseString(param.get("fromId"));
        String to = DPUtil.parseString(param.get("toId"));
        if (DPUtil.empty(from) || DPUtil.empty(to)) return ApiUtil.result(1001, "起点与终点数据不能为空", null);
        if (source.getLabel().equals(target.getLabel()) && from.equals(to)) {
            return ApiUtil.result(1001, "起点与终点是同一个节点，无法查找最短路径", null);
        }
        int maxDepth = ValidateUtil.filterInteger(param.get("maxDepth"), 1, 8, 5);
        List<String> types = new ArrayList<>();
        for (String key : parseIds(param.get("relationships"))) {
            OntologyModel.Relationship relationship = model.relationship(key);
            if (null == relationship) return ApiUtil.result(1002, String.format("关系[%s]未在本体定义中声明", key), null);
            types.add(relationship.getLabel());
        }
        StringBuilder pattern = new StringBuilder("[");
        if (!types.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (String type : types) labels.add(quote(type));
            pattern.append(":").append(DPUtil.implode("|", labels));
        }
        pattern.append("*..").append(maxDepth).append("]");
        // a <> b 必须在 shortestPath 之前过滤：同一对节点（多标签场景）会让 Neo4j 直接抛异常
        String cql = "MATCH (a:" + quote(source.getLabel()) + "), (b:" + quote(target.getLabel()) + ")"
                + " WHERE a." + quote(source.getPrimaryField()) + " = $from AND b." + quote(target.getPrimaryField()) + " = $to"
                + " AND a <> b"
                + " MATCH p = shortestPath((a)-" + pattern + "-(b))"
                + " RETURN p LIMIT 1";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("from", convertValue(source.field(source.getPrimaryField()), DPUtil.toJSON(from)));
        parameters.put("to", convertValue(target.field(target.getPrimaryField()), DPUtil.toJSON(to)));
        try (Session session = driver.session()) {
            List<org.neo4j.driver.Record> records = timedRead(session, cql, Neo4jUtil.parameters(parameters), 30);
            ObjectNode data = DPUtil.objectNode();
            ArrayNode nodes = DPUtil.arrayNode();
            ArrayNode relationships = DPUtil.arrayNode();
            if (!records.isEmpty()) {
                Path path = records.get(0).get("p").asPath();
                for (Node node : path.nodes()) nodes.add(nodeJson(node));
                for (Relationship relationship : path.relationships()) relationships.add(relationshipJson(relationship));
                data.put("length", path.length());
            }
            data.set("nodes", nodes);
            data.set("relationships", relationships);
            data.put("found", nodes.size() > 0);
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 路径推理：指定起止节点与最大深度，返回两点之间的全部节点路径
     *
     * param:
     *  ontologyId    本体标识
     *  fromEntity    起点实体标识，fromId 起点实体主键值
     *  toEntity      终点实体标识，toId 终点实体主键值
     *  relationships 关系类型列表，为空表示任意关系
     *  direction     方向 out/in/both，默认 both
     *  maxDepth      最大深度，1-8，默认 4
     *  limit         最大路径数，默认 50，最大 200
     */
    public Map<String, Object> paths(Map<String, Object> param) {
        OntologyModel model = loadModel(param);
        if (null == model) return ApiUtil.result(1404, "本体不存在", null);
        OntologyModel.Entity source = model.entity(DPUtil.parseString(param.get("fromEntity")));
        OntologyModel.Entity target = model.entity(DPUtil.parseString(param.get("toEntity")));
        if (null == source || null == target) return ApiUtil.result(1404, "起点或终点实体定义不存在", null);
        if (DPUtil.empty(source.getLabel()) || DPUtil.empty(target.getLabel())) {
            return ApiUtil.result(1006, "实体未设置标签，无法进行路径推理", null);
        }
        if (DPUtil.empty(source.getPrimaryField()) || DPUtil.empty(target.getPrimaryField())) {
            return ApiUtil.result(1006, "实体未设置主键字段，无法进行路径推理", null);
        }
        String from = DPUtil.parseString(param.get("fromId"));
        String to = DPUtil.parseString(param.get("toId"));
        if (DPUtil.empty(from) || DPUtil.empty(to)) return ApiUtil.result(1001, "起点与终点数据不能为空", null);
        List<String> types = new ArrayList<>();
        for (String key : parseIds(param.get("relationships"))) {
            OntologyModel.Relationship relationship = model.relationship(key);
            if (null == relationship) return ApiUtil.result(1002, String.format("关系[%s]未在本体定义中声明", key), null);
            types.add(relationship.getLabel());
        }
        int maxDepth = ValidateUtil.filterInteger(param.get("maxDepth"), 1, 8, 4);
        int limit = ValidateUtil.filterInteger(param.get("limit"), 1, 200, 50);
        String direction = DPUtil.parseString(param.get("direction")).toLowerCase();
        String left = "-";
        String right = "->";
        if ("in".equals(direction)) {
            left = "<-";
            right = "-";
        } else if ("both".equals(direction)) {
            right = "-";
        }
        StringBuilder pattern = new StringBuilder("r");
        if (!types.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (String type : types) labels.add(quote(type));
            pattern.append(":").append(DPUtil.implode("|", labels));
        }
        pattern.append("*1..").append(maxDepth);
        String match = "MATCH (a:" + quote(source.getLabel()) + "), (b:" + quote(target.getLabel()) + ")"
                + " WHERE a." + quote(source.getPrimaryField()) + " = $from"
                + " AND b." + quote(target.getPrimaryField()) + " = $to"
                + " AND a <> b";
        String cql = match + " MATCH p = (a)" + left + "[" + pattern + "]" + right + "(b)"
                + " RETURN p ORDER BY length(p) LIMIT $limit";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("from", convertValue(source.field(source.getPrimaryField()), DPUtil.toJSON(from)));
        parameters.put("to", convertValue(target.field(target.getPrimaryField()), DPUtil.toJSON(to)));
        parameters.put("limit", limit);
        try (Session session = driver.session()) {
            ObjectNode nodes = DPUtil.objectNode();
            ObjectNode relationships = DPUtil.objectNode();
            ArrayNode paths = DPUtil.arrayNode();
            for (org.neo4j.driver.Record record : timedRead(session, cql, Neo4jUtil.parameters(parameters), 30)) {
                Path path = record.get("p").asPath();
                ArrayNode nodeIds = DPUtil.arrayNode();
                for (Node node : path.nodes()) {
                    nodes.set(node.elementId(), nodeJson(node));
                    nodeIds.add(node.elementId());
                }
                ArrayNode relationshipIds = DPUtil.arrayNode();
                for (Relationship relationship : path.relationships()) {
                    relationships.set(relationship.elementId(), relationshipJson(relationship));
                    relationshipIds.add(relationship.elementId());
                }
                ObjectNode item = DPUtil.objectNode();
                item.set("nodes", nodeIds);
                item.set("relationships", relationshipIds);
                item.put("length", path.length());
                paths.add(item);
            }
            ObjectNode data = DPUtil.objectNode();
            ArrayNode nodeArray = DPUtil.arrayNode();
            nodes.elements().forEachRemaining(nodeArray::add);
            ArrayNode relationshipArray = DPUtil.arrayNode();
            relationships.elements().forEachRemaining(relationshipArray::add);
            data.set("nodes", nodeArray);
            data.set("relationships", relationshipArray);
            data.set("paths", paths);
            data.put("total", paths.size());
            data.put("truncated", paths.size() >= limit);
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 数据标签巡检：找出实际标签与本体定义不一致的节点
     */
    public Map<String, Object> inspect(Map<String, Object> param) {
        OntologyModel model = loadModel(param);
        if (null == model) return ApiUtil.result(1404, "本体不存在", null);
        ArrayNode rows = DPUtil.arrayNode();
        try (Session session = driver.session()) {
            for (OntologyModel.Entity entity : model.getEntities()) {
                if (DPUtil.empty(entity.getLabel()) || entity.getLabels().isEmpty()) continue;
                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("labels", entity.getLabels());
                String where = "(ANY(l IN labels(n) WHERE NOT (l IN $labels))"
                        + " OR NOT ALL(l IN $labels WHERE l IN labels(n)))";
                String match = "MATCH (n:" + quote(entity.getLabel()) + ") WHERE " + where;
                long total = Neo4jUtil.singleLong(session.run(match + " RETURN COUNT(n)", Neo4jUtil.parameters(parameters)));
                ObjectNode row = entity.toJson();
                row.put("abnormal", total);
                ArrayNode samples = row.putArray("samples");
                if (total > 0) {
                    Map<String, Object> sampleParam = new LinkedHashMap<>(parameters);
                    sampleParam.put("limit", 20);
                    Result result = session.run(match + " RETURN n LIMIT $limit", Neo4jUtil.parameters(sampleParam));
                    while (result.hasNext()) samples.add(nodeJson(result.next().get("n").asNode()));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        return ApiUtil.result(0, null, rows);
    }

    /**
     * 分页大小：导出场景（内部参数）放宽到 50000，普通检索限制 500
     */
    protected int pageSize(Map<String, Object> param, int defaultValue) {
        int max = DPUtil.parseBoolean(param.get("__export")) ? 50000 : 500;
        return ValidateUtil.filterInteger(param.get("pageSize"), 1, max, defaultValue);
    }

    protected long countNodes(Session session, String label) {
        return Neo4jUtil.singleLong(session.run("MATCH (n:" + quote(label) + ") RETURN COUNT(n)"));
    }

    protected long countRelationships(Session session, String label) {
        return Neo4jUtil.singleLong(session.run("MATCH ()-[r:" + quote(label) + "]->() RETURN COUNT(r)"));
    }

    /**
     * 构造属性过滤条件
     */
    protected String appendFilters(String variable, List<OntologyModel.Field> fields, JsonNode filters,
                                   List<String> where, Map<String, Object> parameters) {
        if (null == filters || filters.isNull() || filters.isMissingNode()) return null;
        List<ObjectNode> conditions = new ArrayList<>();
        if (filters.isArray()) {
            for (JsonNode item : filters) {
                if (!item.isObject()) return "查询条件格式不正确";
                ObjectNode condition = DPUtil.objectNode();
                condition.put("field", item.at("/field").asText(item.at("/name").asText("")));
                condition.put("operator", item.at("/operator").asText(item.at("/op").asText("eq")));
                condition.set("value", item.at("/value"));
                conditions.add(condition);
            }
        } else if (filters.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = filters.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                ObjectNode condition = DPUtil.objectNode();
                condition.put("field", entry.getKey());
                condition.put("operator", "eq");
                condition.set("value", entry.getValue());
                conditions.add(condition);
            }
        } else {
            return "查询条件格式不正确";
        }
        for (ObjectNode condition : conditions) {
            String name = DPUtil.trim(condition.at("/field").asText(""));
            String operator = DPUtil.parseString(condition.at("/operator").asText("eq")).toLowerCase();
            JsonNode value = condition.at("/value");
            if (DPUtil.empty(name)) return "查询字段不能为空";
            OntologyModel.Field field = field(fields, name);
            if (null == field) return String.format("字段[%s]未在本体定义中声明", name);
            String property = variable + "." + quote(name);
            String key = "p" + parameters.size();
            switch (operator) {
                case "eq":
                case "=":
                    where.add(property + " = $" + key);
                    parameters.put(key, convertValue(field, value));
                    break;
                case "ne":
                case "!=":
                case "<>":
                    where.add(property + " <> $" + key);
                    parameters.put(key, convertValue(field, value));
                    break;
                case "gt":
                case ">":
                    where.add(property + " > $" + key);
                    parameters.put(key, convertValue(field, value));
                    break;
                case "ge":
                case ">=":
                    where.add(property + " >= $" + key);
                    parameters.put(key, convertValue(field, value));
                    break;
                case "lt":
                case "<":
                    where.add(property + " < $" + key);
                    parameters.put(key, convertValue(field, value));
                    break;
                case "le":
                case "<=":
                    where.add(property + " <= $" + key);
                    parameters.put(key, convertValue(field, value));
                    break;
                case "contains":
                    where.add("toString(" + property + ") CONTAINS $" + key);
                    parameters.put(key, value.asText(""));
                    break;
                case "startswith":
                    where.add("toString(" + property + ") STARTS WITH $" + key);
                    parameters.put(key, value.asText(""));
                    break;
                case "endswith":
                    where.add("toString(" + property + ") ENDS WITH $" + key);
                    parameters.put(key, value.asText(""));
                    break;
                case "in":
                    where.add(property + " IN $" + key);
                    parameters.put(key, convertValue(field, value));
                    break;
                case "isnull":
                case "is_null":
                    where.add(property + " IS NULL");
                    break;
                case "isnotnull":
                case "is_not_null":
                    where.add(property + " IS NOT NULL");
                    break;
                default:
                    return String.format("不支持的查询操作符[%s]", operator);
            }
        }
        return null;
    }

    /**
     * 构造关键字检索条件，匹配本体中字符串类型的字段
     */
    protected String appendKeyword(String variable, OntologyModel.Entity entity, String keyword,
                                   List<String> where, Map<String, Object> parameters) {
        keyword = DPUtil.trim(keyword);
        if (DPUtil.empty(keyword)) return null;
        List<String> fields = new ArrayList<>();
        for (OntologyModel.Field field : entity.getFields()) {
            String type = DPUtil.parseString(field.getType()).toLowerCase();
            if (DPUtil.empty(type) || "string".equals(type) || "text".equals(type)) fields.add(field.getName());
        }
        if (fields.isEmpty() && !DPUtil.empty(entity.getPrimaryField())) fields.add(entity.getPrimaryField());
        if (fields.isEmpty()) return null;
        parameters.put("keyword", keyword);
        parameters.put("keywordFields", fields);
        where.add("ANY(k IN $keywordFields WHERE toString(" + variable + "[k]) CONTAINS $keyword)");
        return null;
    }

    protected OntologyModel.Field field(List<OntologyModel.Field> fields, String name) {
        if (null == fields || DPUtil.empty(name)) return null;
        for (OntologyModel.Field field : fields) {
            if (name.equals(field.getName())) return field;
        }
        return null;
    }

    protected Object convertValue(OntologyModel.Field field, JsonNode value) {
        String type = null == field ? "" : DPUtil.parseString(field.getType());
        if (null == value || value.isNull() || value.isMissingNode()) return null;
        if (value.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : value) list.add(convertScalar(type, item));
            return list;
        }
        return convertScalar(type, value);
    }

    protected Object convertScalar(String type, JsonNode value) {
        if (null == value || value.isNull() || value.isMissingNode()) return null;
        switch (DPUtil.parseString(type)) {
            case "Integer":
            case "int":
            case "Long":
            case "long":
                return value.asLong();
            case "Float":
            case "float":
            case "Double":
            case "double":
            case "Number":
                return value.asDouble();
            case "Boolean":
            case "boolean":
            case "bool":
                return value.asBoolean();
            case "String":
            case "string":
            case "Text":
            case "text":
                return value.asText();
            default:
                if (value.isTextual()) return value.asText();
                if (value.isBoolean()) return value.asBoolean();
                if (value.isIntegralNumber()) return value.asLong();
                if (value.isNumber()) return value.asDouble();
                if (value.isArray()) {
                    List<Object> list = new ArrayList<>();
                    for (JsonNode item : value) list.add(convertScalar("", item));
                    return list;
                }
                if (value.isObject()) return DPUtil.toJSON(value, Map.class);
                return value.asText();
        }
    }

    protected List<JsonNode> parseIdNodes(Object object) {
        List<JsonNode> nodes = new ArrayList<>();
        JsonNode json = DPUtil.toJSON(object);
        if (null == json || json.isNull() || json.isMissingNode()) return nodes;
        if (json.isArray()) {
            for (JsonNode item : json) {
                if (item.isNull()) continue;
                nodes.add(item);
            }
        } else {
            nodes.add(json);
        }
        return nodes;
    }

    protected List<String> parseIds(Object object) {
        List<String> ids = new ArrayList<>();
        for (JsonNode node : parseIdNodes(object)) {
            String id = DPUtil.trim(node.asText(""));
            if (!DPUtil.empty(id)) ids.add(id);
        }
        return ids;
    }

    protected ObjectNode nodeJson(Node node) {
        // elementId 仅用于数据排查与会话内的元素定位（列表行标识、画布渲染），不参与业务逻辑
        return Neo4jUtil.node2json(node);
    }

    protected ObjectNode relationshipJson(Relationship relationship) {
        // elementId 仅用于数据排查与会话内的元素定位（关系删除、画布连线），不参与业务逻辑
        return Neo4jUtil.relationship2json(relationship);
    }

    /**
     * Cypher 标识符转义
     */
    public static String quote(String name) {
        return "`" + DPUtil.parseString(name).replace("`", "") + "`";
    }

}
