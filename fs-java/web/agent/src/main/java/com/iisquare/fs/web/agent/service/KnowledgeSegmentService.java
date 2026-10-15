package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.mvc.ElasticsearchBase;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.agent.dao.KnowledgeChunkDao;
import com.iisquare.fs.web.agent.dao.KnowledgeSegmentDao;
import com.iisquare.fs.web.agent.elasticsearch.KnowledgeChunkES;
import com.iisquare.fs.web.agent.entity.KnowledgeChunk;
import com.iisquare.fs.web.agent.entity.KnowledgeDocument;
import com.iisquare.fs.web.agent.entity.KnowledgeSegment;
import com.iisquare.fs.web.agent.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeSegmentService extends JPAServiceBase {

    @Autowired
    KnowledgeSegmentDao segmentDao;
    @Autowired
    KnowledgeChunkDao chunkDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    KnowledgeService knowledgeService;
    @Autowired
    KnowledgeDocumentService documentService;
    @Autowired
    KnowledgeChunkService chunkService;
    @Autowired
    KnowledgeChunkES chunkES;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "asc");
        sorts.put("status", "asc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public KnowledgeSegment info(Integer id) {
        return info(segmentDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        KnowledgeSegment info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "knowledge", "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "knowledge", "add")) return ApiUtil.result(9403, null, null);
            info = new KnowledgeSegment();
        }
        KnowledgeDocument document = documentService.info(DPUtil.parseInt(param.get("documentId")));
        if (null == document) {
            return ApiUtil.result(2001, "所属文档不存在", null);
        }
        info.setDocumentId(document.getId());
        info.setKnowledgeId(document.getKnowledgeId());
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setTokenSize(DPUtil.parseInt(param.get("tokenSize")));
        info.setStatus(status);
        info = save(segmentDao, info, rbacService.uid(request));
        chunkES.updateSegment(info);
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(segmentDao, param, (root, query, cb) -> {
            SpecificationHelper<KnowledgeSegment> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("knowledgeId");
            helper.equalWithIntGTZero("documentId");
            helper.equalWithIntNotEmpty("status").like("content");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.asc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if(!DPUtil.empty(args.get("withKnowledgeInfo"))) {
            knowledgeService.fillInfo(rows, "knowledgeId");
            documentService.fillInfo(rows, "documentId");
        }
        if(!DPUtil.empty(args.get("withChunk"))) {
            Set<Integer> segmentIds = DPUtil.values(rows, Integer.class, "id");
            if(!segmentIds.isEmpty()) {
                List<KnowledgeChunk> chunkList = chunkDao.findAll((root, query, cb) -> root.get("segmentId").in(segmentIds));
                Map<Integer, List<KnowledgeChunk>> chunkMap = DPUtil.list2ml(chunkList, Integer.class, "segmentId");
                for (JsonNode row : rows) {
                    Integer segmentId = row.get("id").asInt();
                    List<KnowledgeChunk> segmentChunks = chunkMap.get(segmentId);
                    ArrayNode chunkArray = null == segmentChunks ? DPUtil.arrayNode() : DPUtil.toJSON(segmentChunks, ArrayNode.class);
                    fillStatus(chunkArray, chunkService.status());
                    ((ObjectNode) row).set("chunks", chunkArray);
                }
            }
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

    /**
     * 删除分段，先清理下级检索块与 Elasticsearch 记录，索引删除失败时不再继续，便于重试
     */
    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        long indexed = chunkES.deleteByQuery(QueryUtil.terms("segment_id", ids));
        if (ElasticsearchBase.STATE_FAILED == indexed) {
            return ApiUtil.result(1500, "删除检索索引失败", ids);
        }
        Map<String, Object> children = removeByParentId(chunkDao, "segmentId", ids);
        if (ApiUtil.failed(children)) return children;
        return remove(segmentDao, ids);
    }

}
