package com.iisquare.fs.web.lm.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.util.ElasticsearchUtil;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.KnowledgeChunkDao;
import com.iisquare.fs.web.lm.dao.KnowledgeDao;
import com.iisquare.fs.web.lm.dao.KnowledgeDocumentDao;
import com.iisquare.fs.web.lm.dao.KnowledgeSegmentDao;
import com.iisquare.fs.web.lm.elasticsearch.KnowledgeChunkES;
import com.iisquare.fs.web.lm.entity.Knowledge;
import com.iisquare.fs.web.lm.entity.KnowledgeDocument;
import com.iisquare.fs.web.lm.entity.KnowledgeSegment;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeService extends JPAServiceBase {

    @Autowired
    KnowledgeDao knowledgeDao;
    @Autowired
    KnowledgeDocumentDao  knowledgeDocumentDao;
    @Autowired
    KnowledgeSegmentDao knowledgeSegmentDao;
    @Autowired
    KnowledgeChunkDao knowledgeChunkDao;
    @Autowired
    KnowledgeChunkES chunkES;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    AIService aiService;
    @Autowired
    KnowledgeDocumentService knowledgeDocumentService;
    @Autowired
    KnowledgeSegmentService knowledgeSegmentService;

    public Map<String, Object> embedding(Map<String, Object> param) {
        Knowledge knowledge = info(DPUtil.parseInt(param.get("knowledgeId")));
        if (null == knowledge) {
            return ApiUtil.result(1001, "请选择合适的知识库", null);
        }
        if (DPUtil.empty(knowledge.getEmbeddingModel())) {
            return ApiUtil.result(1002, "该知识库暂未配置词嵌入模型", null);
        }
        String content = DPUtil.parseString(param.get("content"));
        return aiService.embeddings(knowledge.getEmbeddingModel(), Collections.singletonList(content));
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> recallTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("vector", "向量检索");
        types.put("text", "全文检索");
        types.put("hybrid", "混合检索");
        return types;
    }

    public Map<String, String> recallScopes() {
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("chunk", "检索块");
        scopes.put("segment", "父子分段");
        scopes.put("document", "全文");
        return scopes;
    }

    public Knowledge info(Integer id) {
        return info(knowledgeDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Knowledge info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", null);
        JsonNode node = DPUtil.firstNode(format(DPUtil.toArrayNode(info)));
        return ApiUtil.result(0, null, node);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "知识库名称异常", name);
        String recallType = DPUtil.parseString(param.get("recallType"));
        if (!recallTypes().containsKey(recallType)) return ApiUtil.result(1005, "召回类型异常", recallType);
        String recallScope = DPUtil.parseString(param.get("recallScope"));
        if (!recallScopes().containsKey(recallScope)) return ApiUtil.result(1002, "召回范围异常", recallScope);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Knowledge info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Knowledge();
        }
        info.setName(name);
        info.setEmbeddingModel(DPUtil.parseString(param.get("embeddingModel")));
        info.setRerankModel(DPUtil.parseString(param.get("rerankModel")));
        info.setTopK(DPUtil.parseInt(param.get("topK")));
        info.setScore(DPUtil.parseDouble(param.get("score")));
        info.setRecallType(recallType);
        info.setRecallScope(recallScope);
        info.setReranked(DPUtil.parseBoolean(param.get("reranked")) ? 1 : 0);
        info.setSplitSeparator(DPUtil.parseString(param.get("splitSeparator")));
        info.setSplitSegmentTokens(DPUtil.parseInt(param.get("splitSegmentTokens")));
        info.setSplitChunkTokens(DPUtil.parseInt(param.get("splitChunkTokens")));
        info.setSplitOverlayTokens(DPUtil.parseInt(param.get("splitOverlayTokens")));
        info.setLabels(DPUtil.implode(",", DPUtil.parseStringList(param.get("labels"))));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(knowledgeDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(knowledgeDao, param, (root, query, cb) -> {
            SpecificationHelper<Knowledge> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
            DPUtil.fillValues(rows, "recallType", "recallTypeText", recallTypes());
            DPUtil.fillValues(rows, "recallScope", "recallScopeText", recallScopes());
        }
        if (!DPUtil.empty(args.get("withRoles"))) {
            rbacService.fillInfos(rows);
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.put("reranked", 1 == node.at("/reranked").asInt(0));
            List<String> labels = DPUtil.parseStringList(node.at("/labels").asText(""));
            node.replace("labels", DPUtil.toJSON(labels));
            List<Integer> ids = DPUtil.parseIntList(node.at("/roleIds").asText(""));
            node.replace("roleIds", DPUtil.toJSON(ids));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        long count = knowledgeDocumentDao.count((Specification<KnowledgeDocument>) (root, query, cb) -> {
            return root.get("knowledgeId").in(ids);
        });
        if (count > 0) return false;
        return remove(knowledgeDao, ids);
    }

    public Map<String, Object> recall(Map<String, Object> param) {
        String query = DPUtil.trim(DPUtil.parseString(param.get("query")));
        if (DPUtil.empty(query)) {
            return ApiUtil.result(1001, "查询内容不能为空", query);
        }
        Knowledge knowledge = info(DPUtil.parseInt(param.get("id")));
        if (null == knowledge || 1 != knowledge.getStatus()) {
            return ApiUtil.result(1002, "当前知识库不可用", param);
        }
        JsonNode embedding;
        if ("text".equals(knowledge.getRecallType())) {
            embedding = DPUtil.arrayNode();
        } else {
            Map<String, Object> embResult = aiService.embeddings(knowledge.getEmbeddingModel(), Collections.singletonList(query));
            if (ApiUtil.failed(embResult)) return embResult;
            embedding = ApiUtil.data(embResult, ObjectNode.class).at("/data/0/embedding");
        }
        List<Query> queries = new ArrayList<>();
        queries.add(QueryUtil.term("knowledge_id", knowledge.getId()));
        queries.add(QueryUtil.term("status", 1));
        queries.add(QueryUtil.term("document_status", 1));
        queries.add(QueryUtil.term("segment_status", 1));
        Map metadata = (Map) param.get("metadata");
        if (null != metadata) {
            metadata.forEach((k, v) -> {
                queries.add(QueryUtil.term("document_metadata", k + "=" + v));
            });
        }
        int topK = DPUtil.parseInt(param.containsKey("topK") ? param.get("topK") : knowledge.getTopK());
        double score = DPUtil.parseDouble(param.containsKey("score") ? param.get("score") : knowledge.getScore());
        topK = topK > 0 ? topK : 10;
        int size = Integer.min(topK * 5, 100);
        ObjectNode sources = ElasticsearchUtil.sources(chunkES.search(s -> {
            s.size(size);
            if (score > 0) s.minScore(score);
            if (!embedding.isEmpty()) {
                s.knn(k -> k.field("embedding").filter(queries)
                        .k(size).numCandidates(100).queryVector(ElasticsearchUtil.vector(embedding)));
            }
            if (Arrays.asList("text", "hybrid").contains(knowledge.getRecallType())) {
                s.query(Query.of(q -> q.bool(b -> {
                    b.must(queries);
                    for (String k : DPUtil.explode("[ ,]", query)) {
                        b.must(QueryUtil.matchPhrase("content", k));
                    }
                    return b;
                })));
            }
            s.source(sr -> sr.filter(f -> f.includes("id", "segment_id", "document_id", "content")));
            return s;
        }), true);
        if (knowledge.getReranked() == 1) {
            Map<String, Object> reranked = rerank(sources, knowledge.getRerankModel(), query, topK, score);
            if (ApiUtil.failed(reranked)) return reranked;
            sources = ApiUtil.data(reranked, ObjectNode.class);
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("id", knowledge.getId());
        result.put("name", knowledge.getName());
        result.put("recallScope", knowledge.getRecallScope());
        ArrayNode chunks = result.putArray("chunks");
        for (JsonNode source : sources) {
            ObjectNode item = chunks.addObject();
            item.replace("id", source.get("id"));
            item.replace("segmentId", source.get("segment_id"));
            item.replace("documentId", source.get("document_id"));
            item.replace("content", source.get("content"));
            item.replace(ElasticsearchUtil.FIELD_SCORE, source.get(ElasticsearchUtil.FIELD_SCORE));
        }
        Set<Integer> segmentIds = DPUtil.values(sources, Integer.class, "segment_id");
        if ("chunk".equals(knowledge.getRecallScope()) || segmentIds.isEmpty()) {
            result.putArray("segments");
        } else {
            result.replace("segments", ServiceUtil.retain(knowledgeSegmentService.format(DPUtil.toJSON(
                    knowledgeSegmentDao.findAll((Specification<KnowledgeSegment>) (root, query1, cb) -> cb.and(
                            root.get("id").in(segmentIds),
                            cb.equal(root.get("status"), 1)
                    ))
            )), "id, documentId, content"));
        }
        Set<Integer> documentIds = DPUtil.values(sources, Integer.class, "document_id");
        if (documentIds.isEmpty()) {
            result.putArray("documents");
        } else {
            result.replace("documents", ServiceUtil.retain(knowledgeDocumentService.format(DPUtil.toJSON(
                    knowledgeDocumentDao.findAll((Specification<KnowledgeDocument>) (root, query1, cb) -> cb.and(
                            root.get("id").in(documentIds),
                            cb.equal(root.get("status"), 1)
                    ))
            )), "id, name, metadata"));
        }
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> rerank(ObjectNode sources, String model, String query, int topN, double score) {
        List<String> documents = new ArrayList<>();
        for (JsonNode source : sources) {
            String text = source.at("/content").asText();
            if (DPUtil.empty(text)) continue;
            documents.add(text);
        }
        if (documents.isEmpty()) {
            return ApiUtil.result(0, null, sources);
        }
        ObjectNode json = DPUtil.json2oo(sources, "content", ElasticsearchUtil.FIELD_ID);
        Map<String, Object> reranked = aiService.rerank(model, query, documents, topN);
        if (ApiUtil.failed(reranked)) return reranked;
        ObjectNode result = DPUtil.objectNode();
        LOOP: for (JsonNode item : ApiUtil.data(reranked, ObjectNode.class).at("/results")) {
            String text = item.at("/document/text").asText();
            double relevanceScore = item.at("/relevance_score").doubleValue();
            if (relevanceScore < score || !json.has(text)) continue;
            for (Map.Entry<String, JsonNode> entry : json.get(text).properties()) {
                if (result.size() >= topN) break LOOP;
                ObjectNode node = (ObjectNode) entry.getValue();
                node.put(ElasticsearchUtil.FIELD_SCORE, relevanceScore);
                result.replace(entry.getKey(), node);
            }
        }
        return ApiUtil.result(0, null, result);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(knowledgeDao, rows, properties);
    }

}
