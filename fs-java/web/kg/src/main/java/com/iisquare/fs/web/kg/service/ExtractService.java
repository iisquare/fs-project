package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.kg.dao.ExtractSourceDao;
import com.iisquare.fs.web.kg.entity.ExtractSource;
import com.iisquare.fs.web.kg.ontology.OntologyModel;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识抽取服务
 *
 * 一个工作台搞定全流程：维护数据源 → 编辑原文 → 预览抽取（识别实体、关系、属性）
 * → 人工采纳 → 入图（按主键 MERGE）。
 *
 * 预览直接使用编辑区当前文本，不落库；只有入图才会写图数据库，且只写人工采纳的候选。
 */
@Service
public class ExtractService extends JPAServiceBase {

    public static final String KIND_ENTITY = "ENTITY";
    public static final String KIND_RELATIONSHIP = "RELATIONSHIP";
    public static final String KIND_ATTRIBUTE = "ATTRIBUTE";

    public static final int STATUS_PENDING = 1;
    public static final int STATUS_ACCEPTED = 2;

    public static final int MAX_TEXT_LENGTH = 200000; // 单次抽取的文本长度上限
    public static final int MAX_DICTIONARY = 20000; // 词典条数上限
    public static final int MAX_SOURCE_ROWS = 5000; // 单个实体的词典抽取上限
    public static final long DICTIONARY_TTL = 60000L; // 图数据词典缓存时长

    /**
     * 图数据词典缓存：ontologyId -> [时间戳, List&lt;DictItem&gt;]
     *
     * 编辑区实时预览会频繁触发抽取，缓存词典避免每次都回图数据库取一遍。
     */
    protected final Map<Integer, Object[]> dictionaryCache = new ConcurrentHashMap<>();

    @Autowired
    protected Driver driver;
    @Autowired
    ExtractSourceDao sourceDao;
    @Autowired
    OntologyService ontologyService;
    @Autowired
    GraphDataService graphDataService;
    @Autowired
    DefaultRbacService rbacService;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("createdTime", "desc");
        sorts.put("processTime", "desc");
        return sorts;
    }

    /* ---------------- 数据源 ---------------- */

    /**
     * 数据源列表：支持名称/类型/处理状态过滤、排序与分页，并带录入人与处理人信息
     *
     * param: keyword、type、processStatus、page、pageSize、sort
     */
    public Map<String, Object> sourceList(Map<String, Object> param) {
        ObjectNode result = search(sourceDao, param, (root, query, cb) -> {
            SpecificationHelper<ExtractSource> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.like("name").equal("type").equalWithIntGTZero("status");
            List<Predicate> predicates = new ArrayList<>(Arrays.asList(helper.predicates()));
            if (param.containsKey("processStatus") && !DPUtil.empty(param.get("processStatus"))) {
                predicates.add(cb.equal(root.get("processStatus"), DPUtil.parseInt(param.get("processStatus"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = ApiUtil.rows(result);
        rbacService.fillUserInfo(rows, "createdUid", "processUid");
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> sourceInfo(Map<String, Object> param) {
        ExtractSource item = sourceDao.findById(DPUtil.parseInt(param.get("id"))).orElse(null);
        if (null == item) return ApiUtil.result(1404, "数据源不存在", null);
        ObjectNode node = DPUtil.objectNode();
        node.put("id", item.getId());
        node.put("name", item.getName());
        node.put("type", item.getType());
        node.put("filename", item.getFilename());
        node.put("size", null == item.getSize() ? 0L : item.getSize());
        node.put("content", item.getContent());
        node.put("processStatus", null == item.getProcessStatus() ? 0 : item.getProcessStatus());
        node.put("processTime", null == item.getProcessTime() ? 0L : item.getProcessTime());
        node.put("processUid", null == item.getProcessUid() ? 0 : item.getProcessUid());
        node.put("createdTime", item.getCreatedTime());
        node.put("createdUid", null == item.getCreatedUid() ? 0 : item.getCreatedUid());
        node.put("updatedTime", item.getUpdatedTime());
        ArrayNode rows = DPUtil.arrayNode();
        rows.add(node);
        rbacService.fillUserInfo(rows, "createdUid", "processUid");
        return ApiUtil.result(0, null, node);
    }

    public Map<String, Object> sourceSave(Map<String, Object> param) {
        String content = DPUtil.parseString(param.get("content"));
        if (DPUtil.empty(content)) return ApiUtil.result(1001, "数据源内容不能为空", null);
        if (content.length() > MAX_TEXT_LENGTH) {
            return ApiUtil.result(1001, String.format("文本长度超过上限%d，请拆分后再抽取", MAX_TEXT_LENGTH), null);
        }
        int uid = DPUtil.parseInt(param.get("uid"));
        long now = System.currentTimeMillis();
        int id = DPUtil.parseInt(param.get("id"));
        ExtractSource item;
        if (id > 0) {
            item = sourceDao.findById(id).orElse(null);
            if (null == item) return ApiUtil.result(1404, "数据源不存在", null);
        } else {
            item = new ExtractSource();
            item.setCreatedTime(now);
            item.setCreatedUid(uid);
        }
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        item.setName(DPUtil.empty(name) ? "未命名数据源" : name);
        item.setType(DPUtil.empty(DPUtil.parseString(param.get("type"))) ? "TEXT" : DPUtil.parseString(param.get("type")).toUpperCase());
        item.setFilename(DPUtil.parseString(param.get("filename")));
        item.setContent(content);
        item.setSize((long) content.length());
        item.setStatus(1);
        if (null == item.getProcessStatus()) item.setProcessStatus(0);
        item.setUpdatedTime(now);
        item.setUpdatedUid(uid);
        item = sourceDao.save(item);
        ObjectNode data = DPUtil.objectNode();
        data.put("id", item.getId());
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> sourceDelete(Map<String, Object> param) {
        Set<Integer> ids = DPUtil.values(DPUtil.toJSON(param.get("ids")), Integer.class);
        if (ids.isEmpty()) return ApiUtil.result(1001, "请选择要删除的数据源", null);
        for (Integer id : ids) sourceDao.deleteById(id);
        return ApiUtil.result(0, null, ids.size());
    }

    /**
     * 标记数据源的处理状态：抽取入图后置为已处理并记录处理人与时间
     *
     * param: id、processStatus（0-未处理，1-已处理）
     */
    public Map<String, Object> sourceMark(Map<String, Object> param) {
        ExtractSource item = sourceDao.findById(DPUtil.parseInt(param.get("id"))).orElse(null);
        if (null == item) return ApiUtil.result(1404, "数据源不存在", null);
        int status = DPUtil.parseInt(param.get("processStatus")) > 0 ? 1 : 0;
        int uid = DPUtil.parseInt(param.get("uid"));
        item.setProcessStatus(status);
        if (status > 0) {
            item.setProcessTime(System.currentTimeMillis());
            item.setProcessUid(uid);
        } else {
            item.setProcessTime(0L);
            item.setProcessUid(0);
        }
        item.setUpdatedTime(System.currentTimeMillis());
        item.setUpdatedUid(uid);
        sourceDao.save(item);
        return ApiUtil.result(0, null, status);
    }

    /* ---------------- 抽取预览 ---------------- */

    /**
     * 预览抽取：直接对传入文本抽一遍，只返回候选不落库，用于编辑区实时预览
     *
     * param: ontologyId、content、entityLabel、threshold、options
     */
    public Map<String, Object> preview(Map<String, Object> param) {
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        OntologyModel model = ontologyService.model(ontologyId);
        if (null == model || model.getEntities().isEmpty()) return ApiUtil.result(1404, "本体不存在或未定义实体", null);
        String content = DPUtil.parseString(param.get("content"));
        if (DPUtil.empty(content)) return ApiUtil.result(1001, "文本内容不能为空", null);
        if (content.length() > MAX_TEXT_LENGTH) {
            return ApiUtil.result(1001, String.format("文本长度超过上限%d，请拆分后再抽取", MAX_TEXT_LENGTH), null);
        }
        double threshold = DPUtil.parseDouble(param.get("threshold"), 0.7D);
        Map<String, Object> options = new LinkedHashMap<>();
        JsonNode optionsNode = DPUtil.toJSON(param.get("options"));
        if (null != optionsNode && optionsNode.isObject()) options = DPUtil.toJSON(optionsNode, Map.class);
        List<Map<String, Object>> results = new ArrayList<>();
        runRule(ontologyId, model, DPUtil.parseString(param.get("entityLabel")), content, options, threshold, results);
        ArrayNode rows = DPUtil.arrayNode();
        for (Map<String, Object> item : results) {
            ObjectNode node = DPUtil.toJSON(item, ObjectNode.class);
            rows.add(node);
        }
        ObjectNode data = DPUtil.objectNode();
        data.set("rows", rows);
        data.put("total", rows.size());
        data.put("entityCount", count(results, KIND_ENTITY));
        data.put("relationshipCount", count(results, KIND_RELATIONSHIP));
        data.put("attributeCount", count(results, KIND_ATTRIBUTE));
        data.put("pendingCount", countStatus(results, STATUS_PENDING));
        return ApiUtil.result(0, null, data);
    }

    /* ---------------- 入图 ---------------- */

    /**
     * 入图：把工作台上人工采纳的候选按主键 MERGE 写入图数据库
     *
     * param:
     *  ontologyId    本体标识
     *  entities      已采纳的实体：[{label, properties}]
     *  relationships 已采纳的关系：[{label, source, target, properties}]
     */
    public Map<String, Object> apply(Map<String, Object> param) {
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        if (ontologyId < 1) return ApiUtil.result(1001, "请选择本体", null);
        int uid = DPUtil.parseInt(param.get("uid"));
        JsonNode entities = DPUtil.toJSON(param.get("entities"));
        JsonNode relationships = DPUtil.toJSON(param.get("relationships"));
        if ((!entities.isArray() || entities.isEmpty()) && (!relationships.isArray() || relationships.isEmpty())) {
            return ApiUtil.result(1001, "没有已采纳的候选，请先采纳后再入图", null);
        }
        ArrayNode issues = DPUtil.arrayNode();
        int entityTotal = 0;
        int relationshipTotal = 0;
        // 实体按标签分组写入（MERGE 主键）
        Map<String, List<Map<String, Object>>> entityGroups = new LinkedHashMap<>();
        if (entities.isArray()) {
            for (JsonNode item : entities) {
                String label = item.at("/label").asText("");
                if (DPUtil.empty(label)) continue;
                Map<String, Object> properties = DPUtil.toJSON(item.at("/properties"), Map.class);
                entityGroups.computeIfAbsent(label, k -> new ArrayList<>()).add(properties);
            }
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : entityGroups.entrySet()) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("ontologyId", ontologyId);
            request.put("entity", entry.getKey());
            request.put("items", entry.getValue());
            request.put("uid", uid);
            Map<String, Object> result = graphDataService.batch(request);
            if (!ApiUtil.succeed(result)) {
                issues.add(issue(KIND_ENTITY, entry.getKey(), ApiUtil.message(result)));
                continue;
            }
            entityTotal += entry.getValue().size();
        }
        // 关系按类型分组写入（两端实体已在上面写好）
        Map<String, List<Map<String, Object>>> relationshipGroups = new LinkedHashMap<>();
        if (relationships.isArray()) {
            for (JsonNode item : relationships) {
                String label = item.at("/label").asText("");
                if (DPUtil.empty(label)) continue;
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("source", item.at("/source").asText(""));
                record.put("target", item.at("/target").asText(""));
                record.put("properties", DPUtil.toJSON(item.at("/properties"), Map.class));
                relationshipGroups.computeIfAbsent(label, k -> new ArrayList<>()).add(record);
            }
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : relationshipGroups.entrySet()) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("ontologyId", ontologyId);
            request.put("relationship", entry.getKey());
            request.put("items", entry.getValue());
            request.put("uid", uid);
            Map<String, Object> result = graphDataService.batch(request);
            if (!ApiUtil.succeed(result)) {
                issues.add(issue(KIND_RELATIONSHIP, entry.getKey(), ApiUtil.message(result)));
                continue;
            }
            relationshipTotal += entry.getValue().size();
        }
        ObjectNode data = DPUtil.objectNode();
        data.put("entityCount", entityTotal);
        data.put("relationshipCount", relationshipTotal);
        data.set("issues", issues);
        return ApiUtil.result(0, null, data);
    }

    protected ObjectNode issue(String kind, String label, String message) {
        ObjectNode node = DPUtil.objectNode();
        node.put("kind", kind);
        node.put("label", label);
        node.put("message", message);
        return node;
    }

    protected int count(List<Map<String, Object>> results, String kind) {
        int total = 0;
        for (Map<String, Object> item : results) if (kind.equals(item.get("kind"))) total++;
        return total;
    }

    protected int countStatus(List<Map<String, Object>> results, int status) {
        int total = 0;
        for (Map<String, Object> item : results) {
            Object value = item.get("status");
            if (null != value && status == DPUtil.parseInt(value)) total++;
        }
        return total;
    }

    /* ---------------- 规则词典抽取 ---------------- */

    /**
     * 词典项：文本、实体标签、主键值与标题
     */
    protected static class DictItem {
        public String text;
        public String label;
        public String primaryValue;
        public String caption;

        public DictItem(String text, String label, String primaryValue, String caption) {
            this.text = text;
            this.label = label;
            this.primaryValue = primaryValue;
            this.caption = caption;
        }
    }

    /**
     * 句子的命中结果：同一段文本里可能出现重复句子，用列表保存避免相互覆盖
     */
    protected static class SentenceHits {
        public String text;
        public int start;
        public List<Map<String, Object>> hits = new ArrayList<>();
    }

    protected void runRule(int ontologyId, OntologyModel model, String target, String text,
                           Map<String, Object> options, double threshold, List<Map<String, Object>> results) {
        List<DictItem> dictionary = buildDictionary(ontologyId, model, options);
        if (dictionary.isEmpty()) return;
        // 最长匹配：按文本长度倒序，并记录已占用的位置，避免短词覆盖长词
        boolean[] used = new boolean[text.length()];
        List<SentenceHits> sentences = new ArrayList<>();
        int cursor = 0;
        for (String sentence : splitSentences(text)) {
            int sentenceStart = text.indexOf(sentence, cursor);
            if (sentenceStart < 0) continue;
            cursor = sentenceStart + sentence.length();
            SentenceHits current = new SentenceHits();
            current.text = sentence;
            current.start = sentenceStart;
            sentences.add(current);
            for (DictItem item : dictionary) {
                int index = sentence.indexOf(item.text);
                while (index >= 0) {
                    int start = sentenceStart + index;
                    int end = start + item.text.length();
                    if (!occupied(used, start, end)) {
                        mark(used, start, end);
                        Map<String, Object> hit = new LinkedHashMap<>();
                        hit.put("start", start);
                        hit.put("end", end);
                        hit.put("text", item.text);
                        hit.put("label", item.label);
                        hit.put("primaryValue", item.primaryValue);
                        hit.put("caption", item.caption);
                        current.hits.add(hit);
                        if (DPUtil.empty(target) || target.equals(item.label)) {
                            OntologyModel.Entity entity = model.entity(item.label);
                            if (null != entity) results.add(entityCandidate(hit, entity, threshold));
                        }
                    }
                    index = sentence.indexOf(item.text, index + 1);
                }
            }
        }
        // 属性抽取：按字段名关键词与数值/日期规则，在同句内取值
        for (SentenceHits item : sentences) {
            for (Map<String, Object> hit : item.hits) {
                OntologyModel.Entity entity = model.entity(DPUtil.parseString(hit.get("label")));
                if (null == entity) continue;
                for (OntologyModel.Field field : entity.getFields()) {
                    Pattern pattern = attributePattern(field);
                    if (null == pattern) continue;
                    Matcher matcher = pattern.matcher(item.text);
                    if (!matcher.find()) continue;
                    Map<String, Object> candidate = attributeCandidate(hit, entity, field, matcher.group(1), threshold);
                    if (null != candidate) results.add(candidate);
                }
            }
        }
        // 关系抽取：同一句内出现的两个实体，若本体中定义了对应关系则生成候选
        Set<String> exists = new LinkedHashSet<>();
        for (SentenceHits item : sentences) {
            for (Map<String, Object> source : item.hits) {
                for (Map<String, Object> targetHit : item.hits) {
                    if (source == targetHit) continue;
                    for (OntologyModel.Relationship relationship : model.getRelationships()) {
                        if (null == relationship.getSourceEntity() || null == relationship.getTargetEntity()) continue;
                        if (!relationship.getSourceEntity().getLabel().equals(source.get("label"))) continue;
                        if (!relationship.getTargetEntity().getLabel().equals(targetHit.get("label"))) continue;
                        String key = relationship.getLabel() + "|" + source.get("primaryValue") + "|" + targetHit.get("primaryValue");
                        if (!exists.add(key)) continue;
                        results.add(relationshipCandidate(relationship, source, targetHit, threshold));
                    }
                }
            }
        }
    }

    /**
     * 词典：图数据中已有的实体标题/主键取值 + 工作台配置的扩展词典
     */
    protected List<DictItem> buildDictionary(int ontologyId, OntologyModel model, Map<String, Object> options) {
        List<DictItem> items = new ArrayList<>(graphDictionary(ontologyId, model));
        Set<String> exists = new LinkedHashSet<>();
        for (DictItem item : items) exists.add(item.label + "|" + item.text);
        // 扩展词典：{"Person":[{"text":"李明","primaryValue":"P001"}]}
        JsonNode dictionary = DPUtil.toJSON(options.get("dictionary"));
        if (dictionary.isObject()) {
            dictionary.fields().forEachRemaining(entry -> {
                JsonNode list = entry.getValue();
                if (!list.isArray()) return;
                for (JsonNode item : list) {
                    String value = DPUtil.parseString(item.isObject() ? item.at("/text").asText("") : item.asText(""));
                    if (DPUtil.empty(value)) continue;
                    String primaryValue = item.isObject() ? item.at("/primaryValue").asText("") : "";
                    if (!exists.add(entry.getKey() + "|" + value)) continue;
                    items.add(new DictItem(value, entry.getKey(), primaryValue, value));
                }
            });
        }
        items.sort((a, b) -> b.text.length() - a.text.length());
        return items;
    }

    /**
     * 图数据词典：按本体缓存实体标题与主键取值，实时预览时避免重复取数
     */
    @SuppressWarnings("unchecked")
    protected List<DictItem> graphDictionary(int ontologyId, OntologyModel model) {
        if (ontologyId > 0) {
            Object[] cached = dictionaryCache.get(ontologyId);
            if (null != cached && System.currentTimeMillis() - (Long) cached[0] < DICTIONARY_TTL) {
                return (List<DictItem>) cached[1];
            }
        }
        List<DictItem> items = loadGraphDictionary(model);
        if (ontologyId > 0) dictionaryCache.put(ontologyId, new Object[]{System.currentTimeMillis(), items});
        return items;
    }

    protected List<DictItem> loadGraphDictionary(OntologyModel model) {
        List<DictItem> items = new ArrayList<>();
        Set<String> exists = new LinkedHashSet<>();
        try (Session session = driver.session()) {
            for (OntologyModel.Entity entity : model.getEntities()) {
                if (DPUtil.empty(entity.getLabel())) continue;
                String primary = entity.getPrimaryField();
                String caption = DPUtil.empty(entity.getCaptionField()) ? primary : entity.getCaptionField();
                if (DPUtil.empty(primary) && DPUtil.empty(caption)) continue;
                String cql = "MATCH (n:" + GraphDataService.quote(entity.getLabel()) + ") RETURN n LIMIT $limit";
                Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("limit", MAX_SOURCE_ROWS);
                try {
                    List<Record> records = session.run(cql, Neo4jUtil.parameters(parameters)).list();
                    for (Record record : records) {
                        org.neo4j.driver.types.Node node = record.get("n").asNode();
                        String primaryValue = node.get(primary).asString("");
                        String captionValue = node.get(caption).asString("");
                        for (String value : new String[]{captionValue, primaryValue}) {
                            if (DPUtil.empty(value) || value.length() < 2) continue;
                            if (!exists.add(entity.getLabel() + "|" + value)) continue;
                            items.add(new DictItem(value, entity.getLabel(), primaryValue, captionValue));
                            if (items.size() >= MAX_DICTIONARY) break;
                        }
                        if (items.size() >= MAX_DICTIONARY) break;
                    }
                } catch (Exception e) {
                    // 单个实体词典加载失败不影响其它实体
                }
                if (items.size() >= MAX_DICTIONARY) break;
            }
        } catch (Exception e) {
            // 词典加载失败时仅使用扩展词典
        }
        return items;
    }

    protected List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (char item : text.toCharArray()) {
            builder.append(item);
            if ('。' == item || '！' == item || '？' == item || '；' == item || '\n' == item || ';' == item) {
                String sentence = DPUtil.trim(builder.toString());
                if (!DPUtil.empty(sentence)) sentences.add(sentence);
                builder.setLength(0);
            }
        }
        String tail = DPUtil.trim(builder.toString());
        if (!DPUtil.empty(tail)) sentences.add(tail);
        return sentences;
    }

    protected boolean occupied(boolean[] used, int start, int end) {
        for (int index = start; index < end && index < used.length; index++) {
            if (used[index]) return true;
        }
        return false;
    }

    protected void mark(boolean[] used, int start, int end) {
        for (int index = start; index < end && index < used.length; index++) used[index] = true;
    }

    /**
     * 候选实体：属性里带上主键字段，保证入图时按主键 MERGE；
     * 扩展词典未配置主键值时，用命中的文本作为主键值。
     */
    protected Map<String, Object> entityCandidate(Map<String, Object> hit, OntologyModel.Entity entity, double threshold) {
        String primaryValue = DPUtil.parseString(hit.get("primaryValue"));
        if (DPUtil.empty(primaryValue)) primaryValue = DPUtil.parseString(hit.get("text"));
        ObjectNode properties = DPUtil.objectNode();
        if (!DPUtil.empty(entity.getPrimaryField()) && !DPUtil.empty(primaryValue)) {
            properties.put(entity.getPrimaryField(), primaryValue);
        }
        ObjectNode payload = DPUtil.objectNode();
        payload.put("text", DPUtil.parseString(hit.get("text")));
        payload.put("start", DPUtil.parseInt(hit.get("start")));
        payload.put("end", DPUtil.parseInt(hit.get("end")));
        payload.set("properties", properties);
        return candidate(KIND_ENTITY, entity.getLabel(), primaryValue, payload, 0.9D, threshold);
    }

    protected Map<String, Object> relationshipCandidate(OntologyModel.Relationship relationship,
                                                        Map<String, Object> source, Map<String, Object> target, double threshold) {
        ObjectNode payload = DPUtil.objectNode();
        payload.put("source", DPUtil.parseString(source.get("primaryValue")));
        payload.put("target", DPUtil.parseString(target.get("primaryValue")));
        payload.put("sourceText", DPUtil.parseString(source.get("text")));
        payload.put("targetText", DPUtil.parseString(target.get("text")));
        payload.set("properties", DPUtil.objectNode());
        String primaryValue = DPUtil.parseString(source.get("primaryValue")) + "->" + DPUtil.parseString(target.get("primaryValue"));
        return candidate(KIND_RELATIONSHIP, relationship.getLabel(), primaryValue, payload, 0.7D, threshold);
    }

    protected Map<String, Object> attributeCandidate(Map<String, Object> hit, OntologyModel.Entity entity,
                                                     OntologyModel.Field field, String value, double threshold) {
        if (DPUtil.empty(value)) return null;
        ObjectNode payload = DPUtil.objectNode();
        payload.put("field", field.getName());
        payload.put("title", DPUtil.empty(field.getTitle()) ? field.getName() : field.getTitle());
        payload.put("value", DPUtil.trim(value));
        payload.put("text", DPUtil.parseString(hit.get("text")));
        return candidate(KIND_ATTRIBUTE, entity.getLabel(), DPUtil.parseString(hit.get("primaryValue")), payload, 0.75D, threshold);
    }

    protected Map<String, Object> candidate(String kind, String label, String primaryValue,
                                            ObjectNode payload, double confidence, double threshold) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("kind", kind);
        item.put("label", label);
        item.put("primaryValue", primaryValue);
        item.put("confidence", confidence);
        item.put("status", confidence >= threshold ? STATUS_ACCEPTED : STATUS_PENDING);
        item.put("payload", DPUtil.toJSON(payload, Object.class));
        return item;
    }

    /**
     * 属性抽取规则：按字段显示名/字段名关键词匹配「字段：值」形态
     */
    protected Pattern attributePattern(OntologyModel.Field field) {
        String title = DPUtil.empty(field.getTitle()) ? field.getName() : field.getTitle();
        String keyword = DPUtil.empty(title) ? field.getName() : title;
        if (DPUtil.empty(keyword)) return null;
        String valuePattern = null;
        String text = keyword + field.getName() + DPUtil.parseString(field.getComment());
        if (text.contains("金额") || text.contains("资本") || text.contains("费用") || text.contains("价格") || text.contains("收入")) {
            valuePattern = "([0-9][0-9,.]*\\s*(?:亿元|万元|元|亿|万))";
        } else if (text.contains("人数") || text.contains("员工") || text.contains("数量") || text.contains("规模") || text.contains("个数")) {
            valuePattern = "([0-9][0-9,]*\\s*(?:人|名|个|家|台|套))";
        } else if (text.contains("日期") || text.contains("时间") || text.contains("成立") || text.contains("年份")) {
            valuePattern = "([0-9]{4}\\s*[-年/.]\\s*[0-9]{1,2}(?:\\s*[-月/.]\\s*[0-9]{1,2}\\s*日?)?)";
        } else if (text.contains("比例") || text.contains("占比") || text.contains("率")) {
            valuePattern = "([0-9][0-9.]*\\s*%)";
        } else if (text.contains("电话") || text.contains("手机")) {
            valuePattern = "(1[3-9][0-9]{9})";
        } else if (text.contains("编号") || text.contains("编码") || text.contains("代码") || text.contains("注册号")) {
            valuePattern = "([A-Za-z0-9\\-]{4,32})";
        }
        if (null == valuePattern) return null;
        return Pattern.compile(Pattern.quote(keyword) + "[^，。；！？\\n]{0,12}?[：:为是]?\\s*" + valuePattern);
    }

    /**
     * 内容摘要，用于列表展示
     */
    protected String summary(String content) {
        String text = DPUtil.trim(DPUtil.parseString(content));
        if (text.length() <= 80) return text;
        return text.substring(0, 80) + "...";
    }

}
