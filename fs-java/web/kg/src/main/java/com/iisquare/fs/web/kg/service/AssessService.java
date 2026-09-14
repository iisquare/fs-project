package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import com.iisquare.fs.web.kg.dao.AssessRecordDao;
import com.iisquare.fs.web.kg.assess.AssessScope;
import com.iisquare.fs.web.kg.entity.AssessRecord;
import com.iisquare.fs.web.kg.entity.Ontology;
import com.iisquare.fs.web.kg.ontology.OntologyModel;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识评估
 *
 * 检查项全部依据本体定义推导：
 * 完整性（主键与必填字段缺失）、唯一性（主键重复）、一致性（标签集合、属性类型、关系端点标签）、规范性（本体未声明的属性、标题字段为空）。
 * 计数类检查精确统计，类型与冗余属性检查按抽样进行，结果中标注是否抽样。
 */
@Service
public class AssessService {

    public static final int DEFAULT_SAMPLE = 2000;
    public static final int SAMPLE_LIMIT = 5;
    public static final long QUERY_TIMEOUT = 60L;

    @Autowired
    protected Driver driver;
    @Autowired
    AssessRecordDao assessRecordDao;
    @Autowired
    OntologyService ontologyService;

    /**
     * 执行评估
     *
     * param: ontologyId 必填，sample 抽样条数（默认2000），save 是否保存记录（默认true）
     */
    public Map<String, Object> assess(Map<String, Object> param, int uid) {
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        Ontology ontology = ontologyService.info(ontologyId);
        if (null == ontology) return ApiUtil.result(1404, "本体不存在", null);
        OntologyModel model = ontologyService.model(ontologyId);
        if (null == model) return ApiUtil.result(1404, "本体定义不存在", null);
        int sample = ValidateUtil.filterInteger(param.get("sample"), 100, 20000, DEFAULT_SAMPLE);
        ArrayNode scopes = DPUtil.arrayNode();
        int issueCount = 0, nodeCount = 0, relationshipCount = 0;
        double scoreTotal = 0D;
        int scoreScopes = 0;
        try (Session session = driver.session()) {
            for (OntologyModel.Entity entity : model.getEntities()) {
                if (DPUtil.empty(entity.getLabel())) continue;
                AssessScope result = assessEntity(session, entity, sample);
                if (null == result || result.count <= 0) continue;
                scopes.add(result.toJson("ENTITY"));
                issueCount += result.issueCount;
                nodeCount += result.count;
                if (result.score >= 0) {
                    scoreTotal += result.score;
                    scoreScopes++;
                }
            }
            for (OntologyModel.Relationship relationship : model.getRelationships()) {
                if (DPUtil.empty(relationship.getLabel()) || null == relationship.getSourceEntity() || null == relationship.getTargetEntity()) continue;
                AssessScope result = assessRelationship(session, relationship, sample);
                if (null == result || result.count <= 0) continue;
                scopes.add(result.toJson("RELATIONSHIP"));
                issueCount += result.issueCount;
                relationshipCount += result.count;
                if (result.score >= 0) {
                    scoreTotal += result.score;
                    scoreScopes++;
                }
            }
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        double score = scoreScopes <= 0 ? 100D : Math.round((scoreTotal / scoreScopes) * 100D) / 100D;
        ObjectNode detail = DPUtil.objectNode();
        detail.put("score", score);
        detail.put("issueCount", issueCount);
        detail.put("nodeCount", nodeCount);
        detail.put("relationshipCount", relationshipCount);
        detail.put("sample", sample);
        detail.set("scopes", scopes);
        detail.set("issues", DPUtil.toJSON(model.getIssues()));
        boolean withSave = !param.containsKey("withSave") || DPUtil.parseBoolean(param.get("withSave"));
        if (withSave) {
            AssessRecord record = new AssessRecord();
            record.setOntologyId(ontologyId);
            record.setOntologyName(ontology.getName());
            record.setScore(score);
            record.setNodeCount(nodeCount);
            record.setRelationshipCount(relationshipCount);
            record.setIssueCount(issueCount);
            record.setDetail(DPUtil.stringify(detail));
            record.setUid(uid);
            record.setCreatedTime(System.currentTimeMillis());
            record = assessRecordDao.save(record);
            detail.put("recordId", record.getId());
        }
        return ApiUtil.result(0, null, detail);
    }

    /**
     * 评估单个实体
     */
    protected AssessScope assessEntity(Session session, OntologyModel.Entity entity, int sample) {
        String label = GraphDataService.quote(entity.getLabel());
        String primaryField = graphField(entity.getPrimaryField());
        long count = Neo4jUtil.singleLong(session.run("MATCH (n:" + label + ") RETURN COUNT(n)",
                Neo4jUtil.parameters(java.util.Collections.emptyMap())));
        if (count <= 0) return null;
        AssessScope result = new AssessScope();
        result.label = entity.getLabel();
        result.name = entity.getName();
        result.primaryField = entity.getPrimaryField();
        result.count = count;
        // 主键缺失
        if (!DPUtil.empty(primaryField)) {
            long issues = count(session, "MATCH (n:" + label + ") WHERE " + primaryField + " IS NULL OR toString(" + primaryField + ") = '' RETURN COUNT(n)");
            result.add("primary", String.format("主键[%s]缺失", entity.getPrimaryField()), count, issues, issues > 0 ? samples(session,
                    "MATCH (n:" + label + ") WHERE " + primaryField + " IS NULL OR toString(" + primaryField + ") = '' RETURN n LIMIT " + SAMPLE_LIMIT, null) : null);
        }
        // 必填字段缺失
        for (OntologyModel.Field field : entity.getFields()) {
            if (!field.isRequired()) continue;
            String property = graphField(field.getName());
            long issues = count(session, "MATCH (n:" + label + ") WHERE " + property + " IS NULL RETURN COUNT(n)");
            result.add("required", String.format("必填字段[%s]缺失", title(field)), count, issues, issues > 0 ? samples(session,
                    "MATCH (n:" + label + ") WHERE " + property + " IS NULL RETURN n LIMIT " + SAMPLE_LIMIT, null) : null);
        }
        // 标题字段为空
        if (!DPUtil.empty(entity.getCaptionField())) {
            String property = graphField(entity.getCaptionField());
            long issues = count(session, "MATCH (n:" + label + ") WHERE " + property + " IS NULL OR toString(" + property + ") = '' RETURN COUNT(n)");
            result.add("caption", String.format("标题字段[%s]为空", entity.getCaptionField()), count, issues, null);
        }
        // 主键重复
        if (!DPUtil.empty(primaryField)) {
            long issues = Neo4jUtil.singleLong(session.run("MATCH (n:" + label + ") WHERE " + primaryField + " IS NOT NULL"
                    + " WITH " + primaryField + " AS value, COUNT(*) AS total WHERE total > 1 RETURN COALESCE(SUM(total - 1), 0)",
                    Neo4jUtil.parameters(java.util.Collections.emptyMap())));
            result.add("unique", String.format("主键[%s]重复", entity.getPrimaryField()), count, issues, issues > 0 ? samples(session,
                    "MATCH (n:" + label + ") WHERE " + primaryField + " IS NOT NULL WITH " + primaryField
                            + " AS value, COUNT(*) AS total WHERE total > 1 RETURN collect(value)[0] AS value LIMIT " + SAMPLE_LIMIT, null) : null);
        }
        // 标签一致性（与本体定义的标签集合比较）
        if (!entity.getLabels().isEmpty()) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("labels", entity.getLabels());
            long issues = Neo4jUtil.singleLong(session.run("MATCH (n:" + label + ")"
                    + " WHERE ANY(l IN labels(n) WHERE NOT (l IN $labels)) OR NOT ALL(l IN $labels WHERE l IN labels(n))"
                    + " RETURN COUNT(n)", Neo4jUtil.parameters(parameters)));
            result.add("label", "实际标签与本体定义不一致", count, issues, issues > 0 ? samples(session,
                    "MATCH (n:" + label + ") WHERE ANY(l IN labels(n) WHERE NOT (l IN $labels))"
                            + " OR NOT ALL(l IN $labels WHERE l IN labels(n)) RETURN n LIMIT " + SAMPLE_LIMIT, parameters) : null);
        }
        // 类型一致性与冗余属性（抽样）
        List<Record> nodes = session.run("MATCH (n:" + label + ") RETURN n LIMIT " + sample,
                Neo4jUtil.parameters(java.util.Collections.emptyMap())).list();
        long typeIssues = 0, extraIssues = 0;
        ArrayNode typeSamples = DPUtil.arrayNode();
        ArrayNode extraSamples = DPUtil.arrayNode();
        for (Record record : nodes) {
            org.neo4j.driver.types.Node node = record.get("n").asNode();
            Map<String, Object> properties = node.asMap();
            for (OntologyModel.Field field : entity.getFields()) {
                Object value = properties.get(field.getName());
                if (null == value) continue;
                if (!matchType(field.getType(), value)) {
                    typeIssues++;
                    if (typeSamples.size() < SAMPLE_LIMIT) typeSamples.add(sampleJson(node, entity.getPrimaryField(),
                            String.format("字段[%s]期望%s，实际%s", field.getName(), field.getType(), value.getClass().getSimpleName())));
                }
            }
            if (!entity.isExtendable()) {
                List<String> extras = new ArrayList<>();
                for (String key : properties.keySet()) {
                    if (null == entity.field(key)) extras.add(key);
                }
                if (!extras.isEmpty()) {
                    extraIssues++;
                    if (extraSamples.size() < SAMPLE_LIMIT) extraSamples.add(sampleJson(node, entity.getPrimaryField(),
                            "存在本体未声明的属性：" + DPUtil.implode(",", extras.toArray(new String[0]))));
                }
            }
        }
        if (nodes.size() > 0) {
            result.add(true, "type", "属性类型与定义不一致（抽样）", nodes.size(), typeIssues, typeSamples);
            if (!entity.isExtendable()) {
                result.add(true, "extra", "存在本体未声明的属性（抽样）", nodes.size(), extraIssues, extraSamples);
            }
        }
        // 孤立节点：无任何关系
        long isolated = count(session, "MATCH (n:" + label + ") WHERE NOT (n)--() RETURN COUNT(n)");
        result.add("isolated", "孤立节点（没有任何关系）", count, isolated, isolated > 0 ? samples(session,
                "MATCH (n:" + label + ") WHERE NOT (n)--() RETURN n LIMIT " + SAMPLE_LIMIT, null) : null);
        return result;
    }

    /**
     * 评估单个关系
     */
    protected AssessScope assessRelationship(Session session, OntologyModel.Relationship relationship, int sample) {
        String type = GraphDataService.quote(relationship.getLabel());
        String sourceLabel = GraphDataService.quote(relationship.getSourceEntity().getLabel());
        String targetLabel = GraphDataService.quote(relationship.getTargetEntity().getLabel());
        String match = "MATCH (a:" + sourceLabel + ")-[r:" + type + "]->(b:" + targetLabel + ")";
        String loose = "MATCH (a)-[r:" + type + "]->(b)";
        long count = Neo4jUtil.singleLong(session.run(loose + " RETURN COUNT(r)",
                Neo4jUtil.parameters(java.util.Collections.emptyMap())));
        if (count <= 0) return null;
        AssessScope result = new AssessScope();
        result.label = relationship.getLabel();
        result.name = relationship.getName();
        result.count = count;
        // 端点标签与定义不一致
        long endpointIssues = count(session, loose + " WHERE NOT (a:" + sourceLabel + ") OR NOT (b:" + targetLabel + ") RETURN COUNT(r)");
        result.add("endpoint", String.format("关系端点不是[%s]->[%s]", relationship.getSourceEntity().getLabel(),
                relationship.getTargetEntity().getLabel()), count, endpointIssues,
                endpointIssues > 0 ? samples(session, loose + " WHERE NOT (a:" + sourceLabel + ") OR NOT (b:" + targetLabel
                        + ") RETURN r, a, b LIMIT " + SAMPLE_LIMIT, null) : null);
        // 必填字段缺失
        for (OntologyModel.Field field : relationship.getFields()) {
            if (!field.isRequired()) continue;
            String property = graphField(field.getName());
            long issues = count(session, "MATCH ()-[r:" + type + "]->() WHERE " + property + " IS NULL RETURN COUNT(r)");
            result.add("required", String.format("必填字段[%s]缺失", title(field)), count, issues, null);
        }
        // 类型一致性与冗余属性（抽样）
        List<Record> rows = session.run(loose + " RETURN r LIMIT " + sample,
                Neo4jUtil.parameters(java.util.Collections.emptyMap())).list();
        long typeIssues = 0, extraIssues = 0;
        ArrayNode typeSamples = DPUtil.arrayNode();
        ArrayNode extraSamples = DPUtil.arrayNode();
        for (Record record : rows) {
            org.neo4j.driver.types.Relationship item = record.get("r").asRelationship();
            Map<String, Object> properties = item.asMap();
            for (OntologyModel.Field field : relationship.getFields()) {
                Object value = properties.get(field.getName());
                if (null == value) continue;
                if (!matchType(field.getType(), value)) {
                    typeIssues++;
                    if (typeSamples.size() < SAMPLE_LIMIT) {
                        ObjectNode node = DPUtil.objectNode();
                        node.put("elementId", item.elementId());
                        node.put("message", String.format("字段[%s]期望%s，实际%s", field.getName(), field.getType(), value.getClass().getSimpleName()));
                        typeSamples.add(node);
                    }
                }
            }
            List<String> extras = new ArrayList<>();
            for (String key : properties.keySet()) {
                if (null == relationship.field(key)) extras.add(key);
            }
            if (!extras.isEmpty()) {
                extraIssues++;
                if (extraSamples.size() < SAMPLE_LIMIT) {
                    ObjectNode node = DPUtil.objectNode();
                    node.put("elementId", item.elementId());
                    node.put("message", "存在本体未声明的属性：" + DPUtil.implode(",", extras.toArray(new String[0])));
                    extraSamples.add(node);
                }
            }
        }
        if (rows.size() > 0) {
            result.add(true, "type", "属性类型与定义不一致（抽样）", rows.size(), typeIssues, typeSamples);
            result.add(true, "extra", "存在本体未声明的属性（抽样）", rows.size(), extraIssues, extraSamples);
        }
        return result;
    }

    /**
     * 历史评估记录
     */
    public Map<String, Object> history(Map<String, Object> param) {
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        ArrayNode rows = DPUtil.arrayNode();
        for (AssessRecord record : assessRecordDao.findAllByOntologyIdOrderByIdDesc(ontologyId)) {
            ObjectNode node = DPUtil.objectNode();
            node.put("id", record.getId());
            node.put("ontologyId", record.getOntologyId());
            node.put("ontologyName", record.getOntologyName());
            node.put("score", record.getScore());
            node.put("nodeCount", record.getNodeCount());
            node.put("relationshipCount", record.getRelationshipCount());
            node.put("issueCount", record.getIssueCount());
            node.put("uid", record.getUid());
            node.put("createdTime", record.getCreatedTime());
            rows.add(node);
        }
        return ApiUtil.result(0, null, rows);
    }

    /**
     * 查看历史评估明细
     */
    public Map<String, Object> detail(Map<String, Object> param) {
        AssessRecord record = assessRecordDao.findById(DPUtil.parseInt(param.get("id"))).orElse(null);
        if (null == record) return ApiUtil.result(1404, "评估记录不存在", null);
        JsonNode detail = DPUtil.parseJSON(record.getDetail());
        if (null == detail || detail.isNull()) detail = DPUtil.objectNode();
        return ApiUtil.result(0, null, detail);
    }

    /* ---------------- 内部方法 ---------------- */

    protected long count(Session session, String cql) {
        return Neo4jUtil.singleLong(session.run(cql, Neo4jUtil.parameters(java.util.Collections.emptyMap())));
    }

    /**
     * 查询问题样本，按节点或关系返回简要信息
     *
     * 样本中的 elementId 仅用于数据排查（页面展示、到图数据管理定位），不参与业务逻辑。
     */
    protected ArrayNode samples(Session session, String cql, Map<String, Object> parameters) {
        ArrayNode samples = DPUtil.arrayNode();
        try {
            Result result = session.run(cql, Neo4jUtil.parameters(null == parameters ? java.util.Collections.emptyMap() : parameters));
            while (result.hasNext() && samples.size() < SAMPLE_LIMIT) {
                Record record = result.next();
                ObjectNode node = DPUtil.objectNode();
                for (String key : record.keys()) {
                    org.neo4j.driver.Value value = record.get(key);
                    if (value instanceof org.neo4j.driver.types.Node) {
                        org.neo4j.driver.types.Node item = value.asNode();
                        ObjectNode json = Neo4jUtil.node2json(item);
                        node.set(key, json);
                    } else if (value instanceof org.neo4j.driver.types.Relationship) {
                        org.neo4j.driver.types.Relationship item = value.asRelationship();
                        ObjectNode json = Neo4jUtil.relationship2json(item);
                        node.set(key, json);
                    } else {
                        node.set(key, Neo4jUtil.value2json(value));
                    }
                }
                samples.add(node);
            }
        } catch (Exception ignored) {
        }
        return samples;
    }

    /**
     * 抽样检查的问题样例，elementId 仅用于数据排查，key 为业务主键值
     */
    protected ObjectNode sampleJson(org.neo4j.driver.types.Node node, String primaryField, String message) {
        ObjectNode json = DPUtil.objectNode();
        json.put("elementId", node.elementId());
        if (!DPUtil.empty(primaryField)) json.put("key", node.get(primaryField).asString(""));
        json.put("message", message);
        return json;
    }

    protected String graphField(String name) {
        return "n." + GraphDataService.quote(name);
    }

    protected String title(OntologyModel.Field field) {
        return DPUtil.empty(field.getTitle()) ? field.getName() : field.getTitle();
    }

    /**
     * 校验属性值类型是否与本体定义一致
     */
    protected boolean matchType(String type, Object value) {
        String expected = DPUtil.parseString(type).toUpperCase();
        if (DPUtil.empty(expected) || "STRING".equals(expected)) {
            return value instanceof String;
        }
        switch (expected) {
            case "INTEGER":
            case "LONG":
                return value instanceof Long || value instanceof Integer;
            case "FLOAT":
            case "DOUBLE":
            case "NUMBER":
                return value instanceof Number;
            case "BOOLEAN":
            case "BOOL":
                return value instanceof Boolean;
            case "LIST":
            case "ARRAY":
                return value instanceof List;
            case "DATE":
            case "DATETIME":
            case "LOCAL DATETIME":
            case "ZONED DATETIME":
                return value instanceof java.time.temporal.Temporal || value instanceof java.time.temporal.TemporalAmount;
            default:
                return true;
        }
    }

}
