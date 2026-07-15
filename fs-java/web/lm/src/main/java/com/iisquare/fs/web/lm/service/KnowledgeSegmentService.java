package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.KnowledgeChunkDao;
import com.iisquare.fs.web.lm.dao.KnowledgeSegmentDao;
import com.iisquare.fs.web.lm.elasticsearch.KnowledgeChunkES;
import com.iisquare.fs.web.lm.entity.KnowledgeChunk;
import com.iisquare.fs.web.lm.entity.KnowledgeDocument;
import com.iisquare.fs.web.lm.entity.KnowledgeSegment;
import com.iisquare.fs.web.lm.mvc.Configuration;
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
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
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
        }, Sort.by(Sort.Order.asc("id")), "id", "status");
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

    public boolean remove(List<Integer> ids) {
        if (!ids.isEmpty()) {
            chunkES.deleteByQuery(QueryUtil.terms("segment_id", ids));
        }
        removeByParentId(chunkDao, "segmentId", ids);
        return remove(segmentDao, ids);
    }

}
