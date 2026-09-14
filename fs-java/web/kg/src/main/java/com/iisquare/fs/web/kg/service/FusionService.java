package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import com.iisquare.fs.web.kg.dao.FusionCandidateDao;
import com.iisquare.fs.web.kg.dao.FusionRecordDao;
import com.iisquare.fs.web.kg.dao.FusionRuleDao;
import com.iisquare.fs.web.kg.dao.FusionTaskDao;
import com.iisquare.fs.web.kg.entity.FusionCandidate;
import com.iisquare.fs.web.kg.entity.FusionRecord;
import com.iisquare.fs.web.kg.entity.FusionRule;
import com.iisquare.fs.web.kg.entity.FusionTask;
import com.iisquare.fs.web.kg.ontology.OntologyModel;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 知识融合
 *
 * 范围限定：同一本体内、同一实体标签的数据去重。
 * 策略：候选一律人工确认；属性冲突保留非空值；不提供撤销，合并过程写入融合记录与变更记录。
 */
@Service
public class FusionService extends JPAServiceBase {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_MERGED = "MERGED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Autowired
    protected Driver driver;
    @Autowired
    FusionRuleDao fusionRuleDao;
    @Autowired
    FusionTaskDao fusionTaskDao;
    @Autowired
    FusionCandidateDao fusionCandidateDao;
    @Autowired
    FusionRecordDao fusionRecordDao;
    @Autowired
    OntologyService ontologyService;
    @Autowired
    DataLogService dataLogService;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("score", "desc");
        sorts.put("createdTime", "desc");
        return sorts;
    }

    protected static class WeightedField {
        public String name = "";
        public double weight = 1D;
    }

    /* ---------------- 规则 ---------------- */

    public Map<String, Object> ruleList(Map<String, Object> param) {
        ArrayNode rows = DPUtil.arrayNode();
        List<FusionRule> rules;
        int status = DPUtil.parseInt(param.get("status"));
        if (status > 0) {
            rules = fusionRuleDao.findAllByStatusOrderByIdDesc(status);
        } else {
            rules = fusionRuleDao.findAll(Sort.by(Sort.Order.desc("id")));
        }
        for (FusionRule rule : rules) rows.add(ruleJson(rule));
        return ApiUtil.result(0, null, rows);
    }

    public Map<String, Object> ruleSave(Map<String, Object> param, int uid) {
        int id = DPUtil.parseInt(param.get("id"));
        FusionRule rule = id > 0 ? info(fusionRuleDao, id) : new FusionRule();
        if (null == rule) return ApiUtil.result(1404, null, id);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "规则名称不能为空", null);
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        if (ontologyId < 1) return ApiUtil.result(1001, "请选择本体", null);
        OntologyModel model = ontologyService.model(ontologyId);
        if (null == model) return ApiUtil.result(1404, "本体不存在", null);
        String entityLabel = DPUtil.trim(DPUtil.parseString(param.get("entityLabel")));
        OntologyModel.Entity entity = model.entity(entityLabel);
        if (null == entity) return ApiUtil.result(1001, "实体定义不存在", entityLabel);
        List<WeightedField> fields = parseFields(DPUtil.toJSON(param.get("fields")));
        if (fields.isEmpty()) return ApiUtil.result(1001, "请选择参与匹配的字段", null);
        for (WeightedField field : fields) {
            if (null == entity.field(field.name)) {
                return ApiUtil.result(1001, String.format("字段[%s]未在实体[%s]中定义", field.name, entityLabel), null);
            }
        }
        rule.setOntologyId(ontologyId);
        rule.setEntityLabel(entity.getLabel());
        rule.setPrimaryField(DPUtil.empty(DPUtil.parseString(param.get("primaryField")))
                ? entity.getPrimaryField() : DPUtil.parseString(param.get("primaryField")));
        rule.setName(name);
        rule.setFields(DPUtil.stringify(DPUtil.toJSON(param.get("fields"))));
        rule.setThreshold(ValidateUtil.filterDouble(param.get("threshold"), 0.1D, 1D, 0.75D));
        rule.setScanLimit(ValidateUtil.filterInteger(param.get("scanLimit"), 10, 20000, 5000));
        rule.setStatus(DPUtil.parseInt(param.get("status")) > 0 ? DPUtil.parseInt(param.get("status")) : 1);
        rule = save(fusionRuleDao, rule, uid);
        return ApiUtil.result(0, null, rule.getId());
    }

    public Map<String, Object> ruleDelete(Map<String, Object> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        if (ids.isEmpty()) return ApiUtil.result(1001, "待删除的标识不能为空", null);
        fusionRuleDao.deleteAllByIdInBatch(ids);
        return ApiUtil.result(0, null, ids.size());
    }

    /* ---------------- 候选扫描 ---------------- */

    public Map<String, Object> scan(Map<String, Object> param, int uid) {
        FusionRule rule = info(fusionRuleDao, DPUtil.parseInt(param.get("ruleId")));
        if (null == rule) return ApiUtil.result(1404, "融合规则不存在", null);
        OntologyModel model = ontologyService.model(rule.getOntologyId());
        OntologyModel.Entity entity = null == model ? null : model.entity(rule.getEntityLabel());
        String label = null == entity ? rule.getEntityLabel() : entity.getLabel();
        String primaryField = DPUtil.empty(rule.getPrimaryField())
                ? (null == entity ? "" : entity.getPrimaryField()) : rule.getPrimaryField();
        if (DPUtil.empty(primaryField)) return ApiUtil.result(1001, "规则未设置主键字段", null);
        List<WeightedField> fields = parseFields(DPUtil.parseJSON(rule.getFields()));
        if (fields.isEmpty()) return ApiUtil.result(1001, "规则未配置匹配字段", null);
        int limit = ValidateUtil.filterInteger(rule.getScanLimit(), 10, 20000, 5000);
        List<ObjectNode> nodes = new ArrayList<>();
        try (Session session = driver.session()) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("limit", limit);
            Result result = session.run("MATCH (n:" + GraphDataService.quote(label) + ") RETURN n LIMIT $limit",
                    Neo4jUtil.parameters(parameters));
            while (result.hasNext() && nodes.size() < limit) {
                ObjectNode node = Neo4jUtil.node2json(result.next().get("n").asNode());
                if (DPUtil.empty(node.at("/properties").at("/" + primaryField).asText(""))) continue;
                nodes.add(node);
            }
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        Set<String> existing = new HashSet<>();
        for (FusionCandidate item : fusionCandidateDao.findAllByOntologyIdAndEntityLabel(rule.getOntologyId(), label)) {
            if (STATUS_REJECTED.equals(item.getStatus())) continue;
            existing.add(item.getLeftKey() + "|" + item.getRightKey());
        }
        double threshold = null == rule.getThreshold() ? 0.75D : rule.getThreshold();
        Map<String, List<Integer>> blocks = new LinkedHashMap<>();
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode properties = nodes.get(index).at("/properties");
            for (WeightedField field : fields) {
                String key = normalize(properties.get(field.name));
                if (DPUtil.empty(key)) continue;
                String blockKey = key.length() > 3 ? key.substring(0, 3) : key;
                blocks.computeIfAbsent(field.name + "|" + blockKey, item -> new ArrayList<>()).add(index);
            }
        }
        Set<String> pairs = new HashSet<>();
        List<FusionCandidate> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (List<Integer> indexes : blocks.values()) {
            if (indexes.size() < 2) continue;
            for (int i = 0; i < indexes.size(); i++) {
                for (int j = i + 1; j < indexes.size(); j++) {
                    int leftIndex = Math.min(indexes.get(i), indexes.get(j));
                    int rightIndex = Math.max(indexes.get(i), indexes.get(j));
                    String pairKey = leftIndex + "|" + rightIndex;
                    if (!pairs.add(pairKey)) continue;
                    String leftKey = nodes.get(leftIndex).at("/properties").at("/" + primaryField).asText("");
                    String rightKey = nodes.get(rightIndex).at("/properties").at("/" + primaryField).asText("");
                    if (leftKey.equals(rightKey) || existing.contains(leftKey + "|" + rightKey)) continue;
                    ArrayNode detail = DPUtil.arrayNode();
                    double score = score(fields, nodes.get(leftIndex).at("/properties"), nodes.get(rightIndex).at("/properties"), detail);
                    if (score < threshold) continue;
                    FusionCandidate candidate = new FusionCandidate();
                    candidate.setRuleId(rule.getId());
                    candidate.setOntologyId(rule.getOntologyId());
                    candidate.setEntityLabel(label);
                    candidate.setLeftKey(leftKey);
                    candidate.setRightKey(rightKey);
                    candidate.setScore(score);
                    candidate.setDetail(DPUtil.stringify(detail));
                    candidate.setStatus(STATUS_PENDING);
                    candidate.setUid(uid);
                    candidate.setCreatedTime(now);
                    candidate.setUpdatedTime(now);
                    candidates.add(candidate);
                }
            }
        }
        FusionTask task = new FusionTask();
        task.setRuleId(rule.getId());
        task.setOntologyId(rule.getOntologyId());
        task.setEntityLabel(label);
        task.setNodeCount(nodes.size());
        task.setCandidateCount(candidates.size());
        task.setUid(uid);
        task.setCreatedTime(now);
        task = fusionTaskDao.save(task);
        for (FusionCandidate candidate : candidates) {
            candidate.setTaskId(task.getId());
            fusionCandidateDao.save(candidate);
        }
        dataLogService.append(rule.getOntologyId(), "ENTITY", label, "FUSION_SCAN",
                String.format("扫描%d个节点，生成%d个候选", nodes.size(), candidates.size()), null, uid,
                0, null);
        ObjectNode data = DPUtil.objectNode();
        data.put("taskId", task.getId());
        data.put("nodeCount", nodes.size());
        data.put("candidateCount", candidates.size());
        return ApiUtil.result(0, null, data);
    }

    /* ---------------- 候选审核 ---------------- */

    public ObjectNode candidateSearch(Map<String, Object> param) {
        return search(fusionCandidateDao, param, (root, query, cb) -> {
            SpecificationHelper<FusionCandidate> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.equalWithIntGTZero("ruleId").equalWithIntGTZero("ontologyId").equal("status").equal("entityLabel");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("score"), Sort.Order.desc("id")), sorts().keySet());
    }

    /**
     * 候选详情：左右数据并排 + 字段对比
     */
    public Map<String, Object> candidateDetail(Map<String, Object> param) {
        FusionCandidate candidate = info(fusionCandidateDao, DPUtil.parseInt(param.get("id")));
        if (null == candidate) return ApiUtil.result(1404, "候选不存在", null);
        OntologyModel model = ontologyService.model(candidate.getOntologyId());
        OntologyModel.Entity entity = null == model ? null : model.entity(candidate.getEntityLabel());
        String primaryField = null == entity ? "" : entity.getPrimaryField();
        if (DPUtil.empty(primaryField)) return ApiUtil.result(1001, "实体未设置主键字段", null);
        ObjectNode left;
        ObjectNode right;
        try (Session session = driver.session()) {
            left = readNode(session, candidate.getEntityLabel(), primaryField, candidate.getLeftKey());
            right = readNode(session, candidate.getEntityLabel(), primaryField, candidate.getRightKey());
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        ArrayNode fields = DPUtil.arrayNode();
        List<OntologyModel.Field> definitions = null == entity ? new ArrayList<>() : entity.getFields();
        for (OntologyModel.Field field : definitions) {
            ObjectNode item = field.toJson();
            String leftValue = DPUtil.parseString(left.at("/properties").at("/" + field.getName()).asText(""));
            String rightValue = DPUtil.parseString(right.at("/properties").at("/" + field.getName()).asText(""));
            item.put("left", leftValue);
            item.put("right", rightValue);
            double similarity = similarity(normalize(leftValue), normalize(rightValue));
            item.put("similarity", similarity < 0 ? 0 : similarity);
            fields.add(item);
        }
        ObjectNode data = DPUtil.objectNode();
        data.put("id", candidate.getId());
        data.put("entityLabel", candidate.getEntityLabel());
        data.put("primaryField", primaryField);
        data.put("leftKey", candidate.getLeftKey());
        data.put("rightKey", candidate.getRightKey());
        data.put("score", candidate.getScore());
        data.put("status", candidate.getStatus());
        data.set("left", left);
        data.set("right", right);
        data.set("fields", fields);
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> reject(Map<String, Object> param, int uid) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        if (ids.isEmpty()) return ApiUtil.result(1001, "待处理的标识不能为空", null);
        long now = System.currentTimeMillis();
        List<FusionCandidate> candidates = fusionCandidateDao.findAllById(ids);
        for (FusionCandidate candidate : candidates) {
            candidate.setStatus(STATUS_REJECTED);
            candidate.setUid(uid);
            candidate.setUpdatedTime(now);
        }
        fusionCandidateDao.saveAll(candidates);
        return ApiUtil.result(0, null, candidates.size());
    }

    /* ---------------- 合并 ---------------- */

    /**
     * 执行合并：候选一律人工确认；属性冲突保留非空值；关系转移并按关系键去重
     */
    public Map<String, Object> merge(Map<String, Object> param, int uid) {
        FusionCandidate candidate = info(fusionCandidateDao, DPUtil.parseInt(param.get("id")));
        if (null == candidate) return ApiUtil.result(1404, "候选不存在", null);
        if (!STATUS_PENDING.equals(candidate.getStatus())) return ApiUtil.result(1001, "该候选已处理", candidate.getStatus());
        boolean keepLeft = !"RIGHT".equalsIgnoreCase(DPUtil.parseString(param.get("keepSide")));
        String keepKey = keepLeft ? candidate.getLeftKey() : candidate.getRightKey();
        String mergedKey = keepLeft ? candidate.getRightKey() : candidate.getLeftKey();
        OntologyModel model = ontologyService.model(candidate.getOntologyId());
        OntologyModel.Entity entity = null == model ? null : model.entity(candidate.getEntityLabel());
        if (null == entity) return ApiUtil.result(1404, "实体定义不存在", candidate.getEntityLabel());
        List<String> mergeFields = new ArrayList<>();
        for (OntologyModel.Relationship relationship : (null == model ? new ArrayList<OntologyModel.Relationship>() : model.getRelationships())) {
            if (null != relationship.getSourceEntity() && candidate.getEntityLabel().equals(relationship.getSourceEntity().getLabel())
                    || null != relationship.getTargetEntity() && candidate.getEntityLabel().equals(relationship.getTargetEntity().getLabel())) {
                for (String field : relationship.getMergeFields()) {
                    if (!mergeFields.contains(field)) mergeFields.add(field);
                }
            }
        }
        String label = entity.getLabel();
        String primaryField = entity.getPrimaryField();
        ObjectNode keepSnapshot;
        ObjectNode mergedSnapshot;
        try (Session session = driver.session()) {
            keepSnapshot = readNode(session, label, primaryField, keepKey);
            mergedSnapshot = readNode(session, label, primaryField, mergedKey);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        if (keepSnapshot.isEmpty() || mergedSnapshot.isEmpty()) {
            return ApiUtil.result(1404, "数据不存在或已被删除，请重新扫描", null);
        }
        Map<String, Object> keepProperties = DPUtil.toJSON(keepSnapshot.at("/properties"), Map.class);
        Map<String, Object> mergedProperties = DPUtil.toJSON(mergedSnapshot.at("/properties"), Map.class);
        Map<String, Object> values = new LinkedHashMap<>();
        Set<String> keys = new LinkedHashSet<>(keepProperties.keySet());
        keys.addAll(mergedProperties.keySet());
        for (String key : keys) {
            Object keepValue = keepProperties.get(key);
            Object mergedValue = mergedProperties.get(key);
            boolean keepEmpty = null == keepValue || DPUtil.empty(keepValue);
            if (keepEmpty && null != mergedValue && !DPUtil.empty(mergedValue)) values.put(key, mergedValue);
        }
        List<String> keepLabels = DPUtil.parseStringList(DPUtil.toJSON(keepSnapshot.at("/labels"), Object.class));
        List<String> mergedLabels = DPUtil.parseStringList(DPUtil.toJSON(mergedSnapshot.at("/labels"), Object.class));
        List<String> extraLabels = new ArrayList<>();
        for (String item : mergedLabels) {
            if (!keepLabels.contains(item)) extraLabels.add(item);
        }
        final Map<String, Object> finalValues = values;
        final List<String> finalExtraLabels = extraLabels;
        long[] counters;
        try (Session session = driver.session()) {
            counters = session.executeWrite(tx -> {
                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("keep", keepKey);
                parameters.put("merged", mergedKey);
                if (!finalValues.isEmpty()) {
                    tx.run("MATCH (k:" + GraphDataService.quote(label) + " {" + GraphDataService.quote(primaryField)
                            + ": $keep}) SET k += $values", parameters(parameters, "values", finalValues)).consume();
                }
                if (!finalExtraLabels.isEmpty()) {
                    StringBuilder sb = new StringBuilder("MATCH (k:" + GraphDataService.quote(label) + " {"
                            + GraphDataService.quote(primaryField) + ": $keep}) SET k");
                    for (String item : finalExtraLabels) sb.append(":").append(GraphDataService.quote(item));
                    tx.run(sb.toString(), Neo4jUtil.parameters(parameters)).consume();
                }
                long[] result = moveRelationships(tx, label, primaryField, keepKey, mergedKey, mergeFields);
                tx.run("MATCH (m:" + GraphDataService.quote(label) + " {" + GraphDataService.quote(primaryField)
                        + ": $merged}) DETACH DELETE m", Neo4jUtil.parameters(parameters)).consume();
                return result;
            });
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), null);
        }
        long now = System.currentTimeMillis();
        FusionRecord record = new FusionRecord();
        record.setCandidateId(candidate.getId());
        record.setOntologyId(candidate.getOntologyId());
        record.setEntityLabel(label);
        record.setKeepKey(keepKey);
        record.setMergedKey(mergedKey);
        record.setKeepSnapshot(DPUtil.stringify(keepSnapshot));
        record.setMergedSnapshot(DPUtil.stringify(mergedSnapshot));
        record.setRelationsMoved((int) counters[0]);
        record.setRelationsMerged((int) counters[1]);
        record.setRelationsRemoved((int) counters[2]);
        record.setLabelsMerged(finalExtraLabels.size());
        record.setUid(uid);
        record.setCreatedTime(now);
        fusionRecordDao.save(record);
        candidate.setStatus(STATUS_MERGED);
        candidate.setUid(uid);
        candidate.setUpdatedTime(now);
        fusionCandidateDao.save(candidate);
        dataLogService.append(candidate.getOntologyId(), "ENTITY", label, "FUSION_MERGE",
                keepKey + "<-" + mergedKey,
                DPUtil.stringify(DPUtil.buildMap("movedRelations", counters[0], "mergedRelations", counters[1],
                        "removedRelations", counters[2], "mergedLabels", finalExtraLabels.size())),
                uid, 0, null);
        ObjectNode data = DPUtil.objectNode();
        data.put("keepKey", keepKey);
        data.put("mergedKey", mergedKey);
        data.put("relationsMoved", counters[0]);
        data.put("relationsMerged", counters[1]);
        data.put("relationsRemoved", counters[2]);
        data.put("labelsMerged", finalExtraLabels.size());
        return ApiUtil.result(0, null, data);
    }

    public ObjectNode recordSearch(Map<String, Object> param) {
        return search(fusionRecordDao, param, (root, query, cb) -> {
            SpecificationHelper<FusionRecord> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.equalWithIntGTZero("ontologyId").equal("entityLabel").like("keepKey").like("mergedKey");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), Arrays.asList("id", "createdTime"));
    }

    /* ---------------- 内部方法 ---------------- */

    protected Map<String, Object> parameters(Map<String, Object> base, String key, Object value) {
        Map<String, Object> parameters = new LinkedHashMap<>(base);
        parameters.put(key, value);
        return parameters;
    }

    /**
     * 转移被合并节点的关系：合并后自环删除，重复关系合并属性，其余转移到保留节点
     */
    protected long[] moveRelationships(TransactionContext tx, String label, String primaryField,
                                       String keepKey, String mergedKey, List<String> mergeFields) {
        long moved = 0, merged = 0, removed = 0;
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("keep", keepKey);
        base.put("merged", mergedKey);
        Record keepRecord = tx.run("MATCH (k:" + GraphDataService.quote(label) + " {"
                + GraphDataService.quote(primaryField) + ": $keep}) RETURN elementId(k) AS id",
                Neo4jUtil.parameters(base)).single();
        String keepId = keepRecord.get("id").asString();
        List<String> types = new ArrayList<>();
        Result typeResult = tx.run("MATCH (m:" + GraphDataService.quote(label) + " {"
                + GraphDataService.quote(primaryField) + ": $merged})-[r]-() RETURN DISTINCT type(r) AS type",
                Neo4jUtil.parameters(base));
        while (typeResult.hasNext()) types.add(typeResult.next().get("type").asString());
        for (String type : types) {
            for (boolean outgoing : new boolean[]{true, false}) {
                String pattern = outgoing
                        ? "MATCH (m:" + GraphDataService.quote(label) + " {" + GraphDataService.quote(primaryField)
                                + ": $merged})-[r:" + GraphDataService.quote(type) + "]->(x)"
                        : "MATCH (m:" + GraphDataService.quote(label) + " {" + GraphDataService.quote(primaryField)
                                + ": $merged})<-[r:" + GraphDataService.quote(type) + "]-(x)";
                List<Record> rows = tx.run(pattern + " RETURN elementId(r) AS rid, properties(r) AS props, elementId(x) AS xid",
                        Neo4jUtil.parameters(base)).list();
                for (Record row : rows) {
                    String rid = row.get("rid").asString();
                    String xid = row.get("xid").asString();
                    Map<String, Object> properties = row.get("props").asMap();
                    Map<String, Object> parameters = new LinkedHashMap<>(base);
                    parameters.put("rid", rid);
                    parameters.put("xid", xid);
                    if (xid.equals(keepId)) {
                        tx.run("MATCH ()-[r]->() WHERE elementId(r) = $rid DELETE r",
                                Neo4jUtil.parameters(parameters)).consume();
                        removed++;
                        continue;
                    }
                    String existing = outgoing
                            ? "MATCH (k:" + GraphDataService.quote(label) + " {" + GraphDataService.quote(primaryField)
                                    + ": $keep})-[r2:" + GraphDataService.quote(type) + "]->(x) WHERE elementId(x) = $xid"
                            : "MATCH (k:" + GraphDataService.quote(label) + " {" + GraphDataService.quote(primaryField)
                                    + ": $keep})<-[r2:" + GraphDataService.quote(type) + "]-(x) WHERE elementId(x) = $xid";
                    List<Record> matches = tx.run(existing + " RETURN elementId(r2) AS rid2, properties(r2) AS props2",
                            Neo4jUtil.parameters(parameters)).list();
                    Record target = null;
                    for (Record match : matches) {
                        if (mergeFields.isEmpty()) {
                            target = match;
                            break;
                        }
                        Map<String, Object> props2 = match.get("props2").asMap();
                        boolean same = true;
                        for (String field : mergeFields) {
                            if (!Objects.equals(properties.get(field), props2.get(field))) {
                                same = false;
                                break;
                            }
                        }
                        if (same) {
                            target = match;
                            break;
                        }
                    }
                    if (null != target) {
                        Map<String, Object> props2 = target.get("props2").asMap();
                        Map<String, Object> fill = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> entry : properties.entrySet()) {
                            Object value = props2.get(entry.getKey());
                            if ((null == value || DPUtil.empty(value)) && null != entry.getValue() && !DPUtil.empty(entry.getValue())) {
                                fill.put(entry.getKey(), entry.getValue());
                            }
                        }
                        if (!fill.isEmpty()) {
                            Map<String, Object> update = new LinkedHashMap<>(parameters);
                            update.put("rid2", target.get("rid2").asString());
                            update.put("values", fill);
                            tx.run("MATCH ()-[r]->() WHERE elementId(r) = $rid2 SET r += $values",
                                    Neo4jUtil.parameters(update)).consume();
                        }
                        tx.run("MATCH ()-[r]->() WHERE elementId(r) = $rid DELETE r",
                                Neo4jUtil.parameters(parameters)).consume();
                        merged++;
                        continue;
                    }
                    String move = outgoing
                            ? "MATCH (x) WHERE elementId(x) = $xid MATCH (k:" + GraphDataService.quote(label) + " {"
                                    + GraphDataService.quote(primaryField) + ": $keep}), ()-[r:" + GraphDataService.quote(type)
                                    + "]->(x) WHERE elementId(r) = $rid CREATE (k)-[nr:" + GraphDataService.quote(type)
                                    + "]->(x) SET nr = properties(r) DELETE r"
                            : "MATCH (x) WHERE elementId(x) = $xid MATCH (k:" + GraphDataService.quote(label) + " {"
                                    + GraphDataService.quote(primaryField) + ": $keep}), ()-[r:" + GraphDataService.quote(type)
                                    + "]-(x) WHERE elementId(r) = $rid CREATE (x)-[nr:" + GraphDataService.quote(type)
                                    + "]->(k) SET nr = properties(r) DELETE r";
                    tx.run(move, Neo4jUtil.parameters(parameters)).consume();
                    moved++;
                }
            }
        }
        return new long[]{moved, merged, removed};
    }

    protected ObjectNode readNode(Session session, String label, String primaryField, String key) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("key", key);
        Result result = session.run("MATCH (n:" + GraphDataService.quote(label) + " {"
                + GraphDataService.quote(primaryField) + ": $key}) RETURN n LIMIT 1", Neo4jUtil.parameters(parameters));
        if (!result.hasNext()) return DPUtil.objectNode();
        // elementId 仅用于数据排查与会话内的元素定位，融合过程按主键匹配
        return Neo4jUtil.node2json(result.next().get("n").asNode());
    }

    protected List<WeightedField> parseFields(JsonNode items) {
        List<WeightedField> fields = new ArrayList<>();
        if (null == items || !items.isArray()) return fields;
        for (JsonNode item : items) {
            String name = DPUtil.trim(item.at("/name").asText(""));
            if (DPUtil.empty(name)) continue;
            WeightedField field = new WeightedField();
            field.name = name;
            field.weight = item.at("/weight").asDouble(1D);
            if (field.weight <= 0) field.weight = 1D;
            fields.add(field);
        }
        return fields;
    }

    /**
     * 加权相似度，任一侧为空时不参与计算
     */
    protected double score(List<WeightedField> fields, JsonNode left, JsonNode right, ArrayNode detail) {
        double total = 0D, weightTotal = 0D;
        for (WeightedField field : fields) {
            String leftText = normalize(left.get(field.name));
            String rightText = normalize(right.get(field.name));
            double similarity = similarity(leftText, rightText);
            ObjectNode item = DPUtil.objectNode();
            item.put("name", field.name);
            item.put("weight", field.weight);
            item.put("left", left.at("/" + field.name).asText(""));
            item.put("right", right.at("/" + field.name).asText(""));
            item.put("score", similarity < 0 ? 0 : similarity);
            detail.add(item);
            if (similarity < 0) continue;
            total += similarity * field.weight;
            weightTotal += field.weight;
        }
        return weightTotal <= 0 ? 0D : total / weightTotal;
    }

    protected double similarity(String left, String right) {
        if (DPUtil.empty(left) || DPUtil.empty(right)) return -1D;
        if (left.equals(right)) return 1D;
        if (left.contains(right) || right.contains(left)) return 0.9D;
        int distance = levenshtein(left, right);
        int max = Math.max(left.length(), right.length());
        return max <= 0 ? 0D : 1D - (double) distance / max;
    }

    protected int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    /**
     * 匹配前的文本规范化：去空格与常见分隔符、全角转半角、转小写
     */
    protected String normalize(JsonNode value) {
        if (null == value || value.isNull()) return "";
        return normalize(value.asText(""));
    }

    protected String normalize(String value) {
        if (DPUtil.empty(value)) return "";
        StringBuilder sb = new StringBuilder();
        for (char item : value.trim().toLowerCase().toCharArray()) {
            char c = item;
            if (c >= 0xFF01 && c <= 0xFF5E) c = (char) (c - 0xFEE0);
            if (Character.isWhitespace(c)) continue;
            if ("-_.()（）[]【】{}".indexOf(c) >= 0) continue;
            sb.append(c);
        }
        return sb.toString();
    }

    protected ObjectNode ruleJson(FusionRule rule) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", rule.getId());
        node.put("name", rule.getName());
        node.put("ontologyId", rule.getOntologyId());
        node.put("entityLabel", rule.getEntityLabel());
        node.put("primaryField", rule.getPrimaryField());
        node.set("fields", DPUtil.parseJSON(rule.getFields()));
        node.put("threshold", rule.getThreshold());
        node.put("scanLimit", rule.getScanLimit());
        node.put("status", rule.getStatus());
        node.put("updatedTime", rule.getUpdatedTime());
        return node;
    }

}
