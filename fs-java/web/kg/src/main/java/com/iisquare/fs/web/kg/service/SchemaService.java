package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import com.iisquare.fs.web.kg.dao.SchemaItemDao;
import com.iisquare.fs.web.kg.entity.Ontology;
import com.iisquare.fs.web.kg.entity.SchemaItem;
import com.iisquare.fs.web.kg.schema.SchemaDefinition;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 图数据库结构治理服务
 *
 * 面向索引与约束的完整生命周期：能力探测、创建前预检、创建与删除、批量执行、登记来源、
 * 与本体方案对账、按差异增量执行。结构管理不受本体定义限制。
 */
@Service
public class SchemaService {

    public static final int STATUS_APPLIED = 1;
    public static final int STATUS_INVALID = 2;

    @Autowired
    protected Driver driver;
    @Autowired
    SchemaItemDao schemaItemDao;
    @Autowired
    OntologyService ontologyService;

    /**
     * 直接查询索引或约束，不做缓存，保证结果始终是数据库当前状态
     */
    protected ArrayNode showRaw(Session session, String kind) {
        return Neo4jUtil.result2json(session.run("SHOW " + kind + " YIELD *"));
    }

    /**
     * 数据库能力探测
     */
    public Map<String, Object> capabilities(Map<String, Object> param) {
        return ApiUtil.result(0, null, capabilitiesNode());
    }

    /**
     * 查询索引与约束，合并登记信息
     *
     * param: kind=INDEX/CONSTRAINT/BOTH，以及name、label、subType、ontologyType、source过滤条件
     */
    public Map<String, Object> show(Map<String, Object> param) {
        String kind = DPUtil.parseString(param.get("kind")).toUpperCase();
        boolean withIndex = DPUtil.empty(kind) || "BOTH".equals(kind) || SchemaDefinition.KIND_INDEX.equals(kind);
        boolean withConstraint = DPUtil.empty(kind) || "BOTH".equals(kind) || SchemaDefinition.KIND_CONSTRAINT.equals(kind);
        Map<String, SchemaItem> registry = registryByName();
        ArrayNode rows = DPUtil.arrayNode();
        try (Session session = driver.session()) {
            if (withIndex) {
                for (JsonNode item : showRaw(session, "INDEXES")) {
                    rows.add(enrich((ObjectNode) item, SchemaDefinition.KIND_INDEX, registry));
                }
            }
            if (withConstraint) {
                for (JsonNode item : showRaw(session, "CONSTRAINTS")) {
                    rows.add(enrich((ObjectNode) item, SchemaDefinition.KIND_CONSTRAINT, registry));
                }
            }
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        ArrayNode filtered = filterRows(rows, param);
        ObjectNode data = DPUtil.objectNode();
        data.set("rows", filtered);
        data.put("total", filtered.size());
        data.set("capabilities", capabilitiesNode());
        return ApiUtil.result(0, null, data);
    }

    /**
     * 结构预检：等价结构、影响数据量、唯一性与存在性冲突、版本能力
     */
    public Map<String, Object> precheck(Map<String, Object> param) {
        SchemaDefinition definition = SchemaDefinition.parse(param);
        String message = definition.validate();
        if (null != message) return ApiUtil.result(1001, message, definition.toJson());
        return ApiUtil.result(0, null, precheckNode(definition));
    }

    /**
     * 创建结构并登记
     *
     * param: 结构定义字段，另支持dryRun-仅预检、force-忽略预检错误
     */
    public Map<String, Object> create(Map<String, Object> param) {
        SchemaDefinition definition = SchemaDefinition.parse(param);
        String message = definition.validate();
        if (null != message) return ApiUtil.result(1001, message, definition.toJson());
        ObjectNode precheck = precheckNode(definition);
        if (DPUtil.parseBoolean(param.get("dryRun"))) return ApiUtil.result(0, null, precheck);
        if (!precheck.at("/errors").isEmpty() && !DPUtil.parseBoolean(param.get("force"))) {
            return ApiUtil.result(1002, "预检未通过，如需强制执行请设置force", precheck);
        }
        try (Session session = driver.session()) {
            session.run(definition.statement()).consume();
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), precheck);
        }
        SchemaItem item = null;
        String registryMessage = null;
        try {
            item = register(definition, DPUtil.parseInt(param.get("uid")));
        } catch (Exception e) {
            registryMessage = e.getMessage();
        }
        precheck.put("registered", null != item);
        if (null != item) {
            precheck.put("registryId", item.getId());
        } else {
            ((ArrayNode) precheck.at("/warnings")).add(
                    String.format("结构已创建，但登记失败[%s]，请确认已执行fs_kg_schema_item建表语句", registryMessage));
        }
        return ApiUtil.result(0, null, precheck);
    }

    /**
     * 删除结构并注销登记
     *
     * 结构名可能来自数据库中已存在的内容（例如手工创建的中文名称），因此只拦截会破坏语句的字符。
     * 删除语句执行失败时，若结构已不存在（例如随约束一并删除的支撑索引），同样视为删除成功。
     */
    public Map<String, Object> drop(Map<String, Object> param) {
        SchemaDefinition definition = SchemaDefinition.parse(param);
        String nameMessage = definition.validateName();
        if (null != nameMessage) return ApiUtil.result(1001, nameMessage, null);
        String statement = definition.dropStatement();
        try (Session session = driver.session()) {
            if (!exists(session, definition)) return ApiUtil.result(0, "结构不存在，无需删除", statement);
            session.run(statement).consume();
        } catch (Exception e) {
            if (!exists(definition)) return ApiUtil.result(0, "结构已不存在，视为删除成功", statement);
            return ApiUtil.result(500, hint(definition, e.getMessage()), statement);
        }
        String registryMessage = null;
        try {
            invalidate(definition.getName(), DPUtil.parseInt(param.get("uid")));
        } catch (Exception e) {
            registryMessage = e.getMessage();
        }
        return ApiUtil.result(0, registryMessage, statement);
    }

    /**
     * 删除失败时补充可操作的提示，例如索引是约束的支撑索引
     */
    protected String hint(SchemaDefinition definition, String message) {
        if (!SchemaDefinition.KIND_INDEX.equals(definition.getKind())) return message;
        try (Session session = driver.session()) {
            if (exists(session, SchemaDefinition.KIND_CONSTRAINT, definition.getName())) {
                return message + "；该索引是约束的支撑索引，请改为删除同名约束";
            }
        } catch (Exception e) {
            return message;
        }
        return message;
    }

    /**
     * 判断索引或约束是否仍然存在，异常时按存在处理，由删除语句给出最终结果
     */
    protected boolean exists(SchemaDefinition definition) {
        try (Session session = driver.session()) {
            return exists(session, definition);
        } catch (Exception e) {
            return true;
        }
    }

    protected boolean exists(Session session, SchemaDefinition definition) {
        return exists(session, definition.getKind(), definition.getName());
    }

    protected boolean exists(Session session, String kind, String name) {
        if (DPUtil.empty(name)) return false;
        String scope = SchemaDefinition.KIND_INDEX.equals(kind) ? "INDEXES" : "CONSTRAINTS";
        for (JsonNode row : showRaw(session, scope)) {
            if (name.equals(row.at("/name").asText(""))) return true;
        }
        return false;
    }

    /**
     * 批量创建与删除，逐条返回结果，单条失败不影响其余执行
     *
     * param: creates-结构定义列表，drops-名称列表或{name}列表
     */
    public Map<String, Object> batch(Map<String, Object> param) {
        ArrayNode results = DPUtil.arrayNode();
        boolean succeed = true;
        int uid = DPUtil.parseInt(param.get("uid"));
        JsonNode creates = DPUtil.toJSON(param.get("creates"));
        if (creates.isArray()) {
            for (JsonNode item : creates) {
                Map<String, Object> itemParam = DPUtil.toJSON(item, Map.class);
                itemParam.put("uid", uid);
                succeed = appendResult(results, "CREATE", safeExecute(itemParam, true)) && succeed;
            }
        }
        JsonNode drops = DPUtil.toJSON(param.get("drops"));
        if (drops.isArray()) {
            for (JsonNode item : drops) {
                Map<String, Object> itemParam = new LinkedHashMap<>();
                String name = item.isObject() ? item.at("/name").asText("") : item.asText("");
                itemParam.put("name", name);
                String kind = item.isObject() ? item.at("/kind").asText("") : "";
                if (DPUtil.empty(kind)) {
                    Optional<SchemaItem> optional = findItem(name);
                    if (optional.isPresent()) kind = optional.get().getKind();
                }
                itemParam.put("kind", kind);
                itemParam.put("uid", uid);
                succeed = appendResult(results, "DROP", safeExecute(itemParam, false)) && succeed;
            }
        }
        return ApiUtil.result(succeed ? 0 : 500, succeed ? null : "部分操作失败", results);
    }

    /**
     * 批量执行时隔离单条异常，避免一条失败影响其余结构的处理
     */
    protected Map<String, Object> safeExecute(Map<String, Object> param, boolean create) {
        try {
            return create ? create(param) : drop(param);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
    }

    /**
     * 校验并返回导入的结构定义清单
     */
    public Map<String, Object> plan(Map<String, Object> param) {
        ArrayNode issues = DPUtil.arrayNode();
        List<SchemaDefinition> definitions = new ArrayList<>();
        JsonNode input = DPUtil.toJSON(param.get("items"));
        if (input.isArray()) {
            for (JsonNode item : input) {
                SchemaDefinition definition = SchemaDefinition.parse(item);
                String message = definition.validate();
                if (null != message) {
                    issues.add(String.format("%s：%s", definition.getName(), message));
                    continue;
                }
                definitions.add(definition);
            }
        } else {
            issues.add("结构定义清单不能为空");
        }
        if (definitions.isEmpty() && !issues.isEmpty()) {
            ObjectNode error = DPUtil.objectNode();
            error.set("issues", issues);
            return ApiUtil.result(1001, "结构定义校验未通过", error);
        }
        ArrayNode items = DPUtil.arrayNode();
        for (SchemaDefinition definition : definitions) items.add(definition.toJson());
        ObjectNode data = DPUtil.objectNode();
        data.set("items", items);
        data.set("issues", issues);
        return ApiUtil.result(0, null, data);
    }

    /**
     * 结构对账：已登记结构与数据库实际结构比对
     *
     * 输出：missing-已登记但数据库中不存在；matched-已登记且结构一致；
     * conflict-同名但结构不一致；unmanaged-数据库中未登记的结构
     */
    public Map<String, Object> diff(Map<String, Object> param) {
        ArrayNode issues = DPUtil.arrayNode();
        List<SchemaDefinition> definitions = expected(param, issues);
        Map<String, SchemaDefinition> actual = actualBySignature();
        Map<String, SchemaDefinition> actualByName = actualByName();
        Map<String, SchemaItem> registry = registryBySignature();
        ArrayNode missing = DPUtil.arrayNode();
        ArrayNode matched = DPUtil.arrayNode();
        ArrayNode conflict = DPUtil.arrayNode();
        ArrayNode unmanaged = DPUtil.arrayNode();
        Set<String> registeredNames = new LinkedHashSet<>();
        for (SchemaDefinition definition : definitions) {
            registeredNames.add(definition.getName());
            SchemaDefinition exist = actual.get(definition.signature());
            if (null == exist) {
                SchemaDefinition byName = actualByName.get(definition.getName());
                if (null == byName) {
                    missing.add(definition.toJson());
                } else {
                    ObjectNode item = definition.toJson();
                    item.put("existingStatement", byName.statement());
                    conflict.add(item);
                }
                continue;
            }
            ObjectNode item = exist.toJson();
            item.put("ontologyId", definition.getOntologyId());
            if (!exist.getName().equals(definition.getName())) item.put("existingName", exist.getName());
            matched.add(item);
        }
        for (Map.Entry<String, SchemaDefinition> entry : actual.entrySet()) {
            if (registry.containsKey(entry.getKey())) continue;
            if (registeredNames.contains(entry.getValue().getName())) continue;
            ObjectNode node = entry.getValue().toJson();
            if (SchemaDefinition.KIND_INDEX.equals(entry.getValue().getKind())
                    && "LOOKUP".equals(entry.getValue().getSubType())) {
                node.put("source", "system");
            } else {
                node.put("source", "unmanaged");
            }
            unmanaged.add(node);
        }
        ObjectNode data = DPUtil.objectNode();
        data.set("missing", missing);
        data.set("matched", matched);
        data.set("conflict", conflict);
        data.set("unmanaged", unmanaged);
        data.set("issues", issues);
        return ApiUtil.result(0, null, data);
    }

    /**
     * 从登记定义重建数据库中缺失的结构
     *
     * param: dryRun-仅返回差异
     */
    public Map<String, Object> apply(Map<String, Object> param) {
        Map<String, Object> diffResult = diff(param);
        if (ApiUtil.failed(diffResult)) return diffResult;
        ObjectNode data = (ObjectNode) diffResult.get(ApiUtil.FIELD_DATA);
        if (DPUtil.parseBoolean(param.get("dryRun"))) return ApiUtil.result(0, null, data);
        int uid = DPUtil.parseInt(param.get("uid"));
        ArrayNode results = DPUtil.arrayNode();
        boolean succeed = true;
        for (JsonNode item : data.at("/missing")) {
            Map<String, Object> itemParam = DPUtil.toJSON(item, Map.class);
            itemParam.put("uid", uid);
            itemParam.put("force", true);
            succeed = appendResult(results, "CREATE", create(itemParam)) && succeed;
        }
        Map<String, Object> after = diff(param);
        ObjectNode result = DPUtil.objectNode();
        result.set("results", results);
        result.set("diff", (JsonNode) after.get(ApiUtil.FIELD_DATA));
        return ApiUtil.result(succeed ? 0 : 500, succeed ? null : "部分操作失败", result);
    }

    /**
     * 全库结构漂移扫描：按登记来源分组，统计已登记但数据库缺失、或同名结构不一致的数量
     */
    public Map<String, Object> scan(Map<String, Object> param) {
        Map<String, SchemaDefinition> actual = actualBySignature();
        Map<String, SchemaDefinition> actualByName = actualByName();
        Map<Integer, ArrayNode> grouped = new LinkedHashMap<>();
        Map<Integer, Integer> missingCount = new LinkedHashMap<>();
        Map<Integer, Integer> conflictCount = new LinkedHashMap<>();
        for (SchemaItem item : registryItems()) {
            int ontologyId = null == item.getOntologyId() ? 0 : item.getOntologyId();
            grouped.computeIfAbsent(ontologyId, key -> DPUtil.arrayNode());
            missingCount.putIfAbsent(ontologyId, 0);
            conflictCount.putIfAbsent(ontologyId, 0);
            SchemaDefinition definition = SchemaDefinition.parse(DPUtil.parseJSON(item.getDefinition()));
            if (DPUtil.empty(definition.getName())) continue;
            SchemaDefinition exist = actual.get(definition.signature());
            if (null == exist) {
                if (null == actualByName.get(definition.getName())) {
                    missingCount.put(ontologyId, missingCount.get(ontologyId) + 1);
                } else {
                    conflictCount.put(ontologyId, conflictCount.get(ontologyId) + 1);
                }
                continue;
            }
            if (!exist.getName().equals(definition.getName())) conflictCount.put(ontologyId, conflictCount.get(ontologyId) + 1);
        }
        ArrayNode rows = DPUtil.arrayNode();
        for (Map.Entry<Integer, ArrayNode> entry : grouped.entrySet()) {
            int ontologyId = entry.getKey();
            ObjectNode row = DPUtil.objectNode();
            row.put("ontologyId", ontologyId);
            row.put("name", ontologyId > 0 ? name(ontologyId) : "手工创建");
            row.put("registered", entry.getValue().size());
            row.put("missing", missingCount.getOrDefault(ontologyId, 0));
            row.put("conflict", conflictCount.getOrDefault(ontologyId, 0));
            rows.add(row);
        }
        return ApiUtil.result(0, null, rows);
    }

    protected String name(Integer ontologyId) {
        Ontology ontology = ontologyService.info(ontologyId);
        return null == ontology ? String.valueOf(ontologyId) : ontology.getName();
    }

    /**
     * 将数据库中已存在的等价结构登记到指定本体，用于处理命名冲突
     */
    public Map<String, Object> attach(Map<String, Object> param) {
        String name = DPUtil.parseString(param.get("name"));
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "结构名称不能为空", null);
        try (Session session = driver.session()) {
            for (JsonNode row : showRaw(session, "INDEXES")) {
                if (!name.equals(row.at("/name").asText(""))) continue;
                return attachItem(SchemaDefinition.fromIndexRow(row), ontologyId, param);
            }
            for (JsonNode row : showRaw(session, "CONSTRAINTS")) {
                if (!name.equals(row.at("/name").asText(""))) continue;
                return attachItem(SchemaDefinition.fromConstraintRow(row), ontologyId, param);
            }
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        return ApiUtil.result(1404, "数据库中不存在该结构", name);
    }

    protected Map<String, Object> attachItem(SchemaDefinition definition, int ontologyId, Map<String, Object> param) {
        definition.setOntologyId(ontologyId);
        SchemaItem item;
        try {
            item = register(definition, DPUtil.parseInt(param.get("uid")));
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        ObjectNode data = definition.toJson();
        data.put("registryId", item.getId());
        data.put("ontologyId", ontologyId);
        return ApiUtil.result(0, null, data);
    }

    /**
     * 期望结构来自登记表：登记内容代表"数据库中应当存在的结构"，与本体定义无关
     */
    protected List<SchemaDefinition> expected(Map<String, Object> param, ArrayNode issues) {
        List<SchemaDefinition> definitions = new ArrayList<>();
        JsonNode items = DPUtil.toJSON(param.get("items"));
        if (items.isArray()) {
            for (JsonNode item : items) {
                SchemaDefinition definition = SchemaDefinition.parse(item);
                String message = definition.validate();
                if (null != message) {
                    issues.add(String.format("%s：%s", definition.getName(), message));
                    continue;
                }
                definitions.add(definition);
            }
            return definitions;
        }
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        for (SchemaItem item : registryItems()) {
            if (ontologyId > 0 && (null == item.getOntologyId() || item.getOntologyId() != ontologyId)) continue;
            SchemaDefinition definition = SchemaDefinition.parse(DPUtil.parseJSON(item.getDefinition()));
            if (DPUtil.empty(definition.getName())) {
                issues.add("登记结构定义解析失败：" + item.getName());
                continue;
            }
            definition.setOntologyId(null == item.getOntologyId() ? 0 : item.getOntologyId());
            definitions.add(definition);
        }
        return definitions;
    }

    /**
     * 数据库实际结构按名称索引
     */
    protected Map<String, SchemaDefinition> actualByName() {
        Map<String, SchemaDefinition> result = new LinkedHashMap<>();
        for (SchemaDefinition definition : actualBySignature().values()) {
            if (DPUtil.empty(definition.getName())) continue;
            result.put(definition.getName(), definition);
        }
        return result;
    }

    /**
     * 创建前预检
     */
    protected ObjectNode precheckNode(SchemaDefinition definition) {
        ObjectNode result = DPUtil.objectNode();
        ArrayNode errors = result.putArray("errors");
        ArrayNode warnings = result.putArray("warnings");
        result.set("definition", definition.toJson());
        result.put("enterpriseOnly", definition.enterpriseOnly());
        ObjectNode capabilities = capabilitiesNode();
        boolean enterprise = capabilities.at("/enterprise").asBoolean(false);
        if (definition.enterpriseOnly() && !enterprise) {
            warnings.add(String.format("结构类型[%s]为企业版特性，当前版本[%s]可能不支持",
                    definition.getSubType(), capabilities.at("/edition").asText("unknown")));
        }
        if (definition.getLabels().size() > 1) {
            warnings.add("该结构作用于多标签组合，请确认当前 Neo4j 版本支持组合标签的约束与索引");
        }
        SchemaDefinition exist = actualBySignature().get(definition.signature());
        if (null != exist && !exist.getName().equals(definition.getName())) {
            warnings.add(String.format("已存在等价结构[%s]，重复创建不会新增", exist.getName()));
        }
        if (!"LOOKUP".equals(definition.getSubType())) {
            long affected = countEntities(definition);
            result.put("affected", affected);
            if (affected < 1) {
                warnings.add(String.format("标签[%s]当前没有数据，请确认拼写", definition.getLabel()));
            }
        }
        if (SchemaDefinition.KIND_CONSTRAINT.equals(definition.getKind())
                && ("UNIQUE".equals(definition.getSubType()) || "KEY".equals(definition.getSubType()))) {
            ArrayNode duplicates = findDuplicates(definition, 5);
            result.set("duplicates", duplicates);
            if (!duplicates.isEmpty()) {
                errors.add("存在重复数据，无法创建该约束，请先完成数据去重");
            }
        }
        if ("NOT_NULL".equals(definition.getSubType())) {
            long missing = countMissing(definition);
            result.put("missing", missing);
            if (missing > 0) {
                errors.add(String.format("存在 %d 条数据缺少属性[%s]", missing, definition.getFields().get(0)));
            }
        }
        result.put("passed", errors.isEmpty());
        return result;
    }

    protected long countEntities(SchemaDefinition definition) {
        String cql = definition.isNode()
                ? "MATCH (n:`" + definition.getLabel() + "`) RETURN COUNT(n)"
                : "MATCH ()-[r:`" + definition.getLabel() + "`]->() RETURN COUNT(r)";
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(cql));
        } catch (Exception e) {
            return -1;
        }
    }

    protected long countMissing(SchemaDefinition definition) {
        String variable = definition.isNode() ? "n" : "r";
        String match = definition.isNode()
                ? "MATCH (n:`" + definition.getLabel() + "`)"
                : "MATCH ()-[r:`" + definition.getLabel() + "`]->()";
        String cql = match + " WHERE " + variable + ".`" + definition.getFields().get(0) + "` IS NULL RETURN COUNT(" + variable + ")";
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(cql));
        } catch (Exception e) {
            return -1;
        }
    }

    protected ArrayNode findDuplicates(SchemaDefinition definition, int limit) {
        ArrayNode result = DPUtil.arrayNode();
        String variable = definition.isNode() ? "n" : "r";
        String match = definition.isNode()
                ? "MATCH (n:`" + definition.getLabel() + "`)"
                : "MATCH ()-[r:`" + definition.getLabel() + "`]->()";
        List<String> selections = new ArrayList<>();
        List<String> aliases = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        int index = 0;
        for (String field : definition.getFields()) {
            String alias = "v" + index++;
            selections.add(variable + ".`" + field + "` AS " + alias);
            aliases.add(alias);
            conditions.add(variable + ".`" + field + "` IS NOT NULL");
        }
        String cql = match + " WHERE " + DPUtil.implode(" AND ", conditions.toArray(new String[0]))
                + " WITH " + DPUtil.implode(", ", selections.toArray(new String[0])) + ", COUNT(*) AS total"
                + " WHERE total > 1 RETURN " + DPUtil.implode(", ", aliases.toArray(new String[0])) + ", total"
                + " ORDER BY total DESC LIMIT " + limit;
        try (Session session = driver.session()) {
            return Neo4jUtil.result2json(session.run(cql));
        } catch (Exception e) {
            return result;
        }
    }

    /**
     * 数据库版本与版本类型
     */
    protected ObjectNode capabilitiesNode() {
        ObjectNode data = DPUtil.objectNode();
        data.put("edition", "unknown");
        data.put("version", "");
        data.put("name", "");
        try (Session session = driver.session()) {
            Result result = session.run("CALL dbms.components() YIELD name, versions, edition RETURN name, versions, edition");
            if (result.hasNext()) {
                Record record = result.next();
                data.put("name", record.get("name").asString());
                data.put("edition", record.get("edition").asString());
                List<String> versions = record.get("versions").asList(Value::asString);
                data.put("version", versions.isEmpty() ? "" : versions.get(0));
            }
        } catch (Exception e) {
            data.put("message", e.getMessage());
        }
        boolean enterprise = data.at("/edition").asText("").toLowerCase().contains("enterprise");
        data.put("enterprise", enterprise);
        ArrayNode indexTypes = data.putArray("indexTypes");
        for (String type : SchemaDefinition.INDEX_TYPES) indexTypes.add(type);
        ArrayNode constraintTypes = data.putArray("constraintTypes");
        ArrayNode enterpriseTypes = data.putArray("enterpriseTypes");
        for (String type : SchemaDefinition.CONSTRAINT_TYPES) {
            if (SchemaDefinition.ENTERPRISE_TYPES.contains(type)) {
                enterpriseTypes.add(type);
                if (!enterprise) continue;
            }
            constraintTypes.add(type);
        }
        return data;
    }

    /**
     * 数据库实际结构，按结构签名索引，约束自带的索引不单独参与对账
     */
    protected Map<String, SchemaDefinition> actualBySignature() {
        Map<String, SchemaDefinition> result = new LinkedHashMap<>();
        try (Session session = driver.session()) {
            for (JsonNode row : showRaw(session, "INDEXES")) {
                if (!row.at("/owningConstraint").asText("").isEmpty()) continue;
                SchemaDefinition definition = SchemaDefinition.fromIndexRow(row);
                result.put(definition.signature(), definition);
            }
            for (JsonNode row : showRaw(session, "CONSTRAINTS")) {
                SchemaDefinition definition = SchemaDefinition.fromConstraintRow(row);
                result.put(definition.signature(), definition);
            }
        } catch (Exception e) {
            return result;
        }
        return result;
    }

    protected Map<String, SchemaItem> registryByName() {
        Map<String, SchemaItem> result = new LinkedHashMap<>();
        for (SchemaItem item : registryItems()) result.put(item.getName(), item);
        return result;
    }

    protected Map<String, SchemaItem> registryBySignature() {
        Map<String, SchemaItem> result = new LinkedHashMap<>();
        for (SchemaItem item : registryItems()) {
            SchemaDefinition definition = SchemaDefinition.parse(DPUtil.parseJSON(item.getDefinition()));
            if (DPUtil.empty(definition.getName())) continue;
            result.put(definition.signature(), item);
        }
        return result;
    }

    protected List<SchemaItem> registryItems() {
        try {
            return schemaItemDao.findAllByStatus(STATUS_APPLIED);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    protected ObjectNode enrich(ObjectNode row, String kind, Map<String, SchemaItem> registry) {
        SchemaDefinition definition = SchemaDefinition.KIND_INDEX.equals(kind)
                ? SchemaDefinition.fromIndexRow(row) : SchemaDefinition.fromConstraintRow(row);
        row.put("kind", kind);
        row.set("definition", definition.toJson());
        SchemaItem item = registry.get(definition.getName());
        if (null == item) {
            boolean system = SchemaDefinition.KIND_INDEX.equals(kind) && "LOOKUP".equals(definition.getSubType());
            row.put("source", system ? "system" : "unmanaged");
            row.put("ontologyId", 0);
            row.put("registered", false);
        } else {
            row.put("source", item.getOntologyId() > 0 ? "ontology" : "manual");
            row.put("ontologyId", item.getOntologyId());
            row.put("registered", true);
            row.put("registryId", item.getId());
            row.put("createdTime", item.getCreatedTime());
            row.put("updatedTime", item.getUpdatedTime());
        }
        return row;
    }

    protected ArrayNode filterRows(ArrayNode rows, Map<String, Object> param) {
        ArrayNode result = DPUtil.arrayNode();
        String name = DPUtil.trim(DPUtil.parseString(param.get("name"))).toLowerCase();
        String label = DPUtil.trim(DPUtil.parseString(param.get("label")));
        String subType = DPUtil.parseString(param.get("subType")).toUpperCase();
        if (DPUtil.empty(subType)) subType = DPUtil.parseString(param.get("type")).toUpperCase();
        String ontologyType = DPUtil.parseString(param.get("ontologyType")).toUpperCase();
        String source = DPUtil.parseString(param.get("source")).toLowerCase();
        String kind = DPUtil.parseString(param.get("kind")).toUpperCase();
        for (JsonNode node : rows) {
            ObjectNode row = (ObjectNode) node;
            if (!DPUtil.empty(name) && !row.at("/name").asText("").toLowerCase().contains(name)) continue;
            if (!DPUtil.empty(label) && !row.at("/definition/label").asText("").contains(label)) continue;
            if (!DPUtil.empty(subType) && !subType.equals(row.at("/definition/subType").asText(""))) continue;
            if (!DPUtil.empty(ontologyType) && !SchemaDefinition.ontologyType(ontologyType).equals(row.at("/definition/ontologyType").asText(""))) continue;
            if (!DPUtil.empty(source) && !source.equals(row.at("/source").asText(""))) continue;
            if (!DPUtil.empty(kind) && !"BOTH".equals(kind) && !kind.equals(row.at("/kind").asText(""))) continue;
            result.add(row);
        }
        return result;
    }

    protected SchemaItem register(SchemaDefinition definition, int uid) {
        SchemaItem item = findItem(definition.getName()).orElseGet(SchemaItem::new);
        long time = System.currentTimeMillis();
        if (null == item.getId()) {
            item.setName(definition.getName());
            item.setCreatedTime(time);
            item.setCreatedUid(uid);
        }
        item.setKind(definition.getKind());
        item.setSubType(definition.getSubType());
        item.setOntologyId(definition.getOntologyId());
        item.setOntologyType(definition.getOntologyType());
        item.setLabel(definition.getLabel());
        item.setFields(DPUtil.implode(",", definition.getFields().toArray(new String[0])));
        item.setPropertyType(definition.getPropertyType());
        item.setDefinition(DPUtil.stringify(definition.toJson()));
        item.setCreateStatement(definition.statement());
        item.setStatus(STATUS_APPLIED);
        item.setUpdatedTime(time);
        item.setUpdatedUid(uid);
        return schemaItemDao.save(item);
    }

    protected void invalidate(String name, int uid) {
        Optional<SchemaItem> optional = findItem(name);
        if (optional.isEmpty()) return;
        SchemaItem item = optional.get();
        item.setStatus(STATUS_INVALID);
        item.setUpdatedTime(System.currentTimeMillis());
        item.setUpdatedUid(uid);
        schemaItemDao.save(item);
    }

    protected Optional<SchemaItem> findItem(String name) {
        try {
            return schemaItemDao.findByName(name);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    protected boolean appendResult(ArrayNode results, String action, Map<String, Object> result) {
        ObjectNode node = DPUtil.objectNode();
        node.put("action", action);
        node.put("code", ApiUtil.code(result));
        node.put("message", ApiUtil.message(result));
        Object data = result.get(ApiUtil.FIELD_DATA);
        // data可能是字符串（如删除语句、结构名称），统一转为JSON节点，避免强制转换异常
        if (null == data) {
            node.putNull("data");
        } else {
            node.set("data", DPUtil.toJSON(data));
        }
        results.add(node);
        return ApiUtil.succeed(result);
    }

}
