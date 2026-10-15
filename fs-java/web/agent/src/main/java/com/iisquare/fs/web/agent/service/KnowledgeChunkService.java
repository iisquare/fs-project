package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.mvc.ElasticsearchBase;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.sse.MaintainEmitter;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.agent.dao.KnowledgeChunkDao;
import com.iisquare.fs.web.agent.dao.KnowledgeDocumentDao;
import com.iisquare.fs.web.agent.dao.KnowledgeSegmentDao;
import com.iisquare.fs.web.agent.elasticsearch.KnowledgeChunkES;
import com.iisquare.fs.web.agent.entity.KnowledgeChunk;
import com.iisquare.fs.web.agent.entity.KnowledgeDocument;
import com.iisquare.fs.web.agent.entity.KnowledgeSegment;
import com.iisquare.fs.web.agent.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class KnowledgeChunkService extends JPAServiceBase {

    @Autowired
    KnowledgeChunkDao chunkDao;
    @Autowired
    KnowledgeSegmentDao segmentDao;
    @Autowired
    KnowledgeDocumentDao documentDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    KnowledgeService knowledgeService;
    @Autowired
    KnowledgeDocumentService documentService;
    @Autowired
    KnowledgeChunkES chunkES;

    /** 检索块索引重建锁：维护任务串行执行，避免重复写入同一批索引 */
    private final AtomicBoolean reindexing = new AtomicBoolean(false);

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

    public KnowledgeChunk info(Integer id) {
        return info(chunkDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        KnowledgeChunk info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "knowledge", "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "knowledge", "add")) return ApiUtil.result(9403, null, null);
            info = new KnowledgeChunk();
        }
        KnowledgeSegment segment = info(segmentDao, DPUtil.parseInt(param.get("segmentId")));
        if (null == segment) {
            return ApiUtil.result(2001, "所属分段不存在", null);
        }
        info.setSegmentId(segment.getId());
        info.setDocumentId(segment.getDocumentId());
        info.setKnowledgeId(segment.getKnowledgeId());
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setEmbedding(DPUtil.parseString(param.get("embedding")));
        info.setStatus(status);
        info = save(chunkDao, info, rbacService.uid(request));
        // 同步写入 Elasticsearch，冗余文档标题、元数据与文档/分段状态
        chunkES.add(chunkES.format(info, segment, documentService.info(info.getDocumentId())));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(chunkDao, param, (root, query, cb) -> {
            SpecificationHelper<KnowledgeChunk> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("knowledgeId");
            helper.equalWithIntGTZero("documentId");
            helper.equalWithIntGTZero("segmentId");
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
        return result;
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

    /**
     * 重建检索块索引：分页把数据库中的分块连同文档、分段状态写入 Elasticsearch
     * 用于修复历史数据缺失 document_status / segment_status 等字段导致的召回失效
     * 执行过程参照 BI 维护任务，以 SSE 输出计划、进度与结果，knowledgeId 为空时重建全部
     */
    public MaintainEmitter reindex(Map<?, ?> param, MaintainEmitter emitter) {
        if (null == emitter || !emitter.isRunning()) return emitter;
        if (!reindexing.compareAndSet(false, true)) {
            emitter.error(1502, "检索块索引正在重建中", null);
            return emitter;
        }
        try {
            Integer knowledgeId = ValidateUtil.filterInteger(param.get("knowledgeId"), true, 1, null, 0);
            Map<String, String> steps = new LinkedHashMap<>();
            steps.put("count", "统计待重建检索块");
            steps.put("index", "重建检索块索引");
            emitter.plan(steps);
            emitter.start("开始重建检索块索引", "reindexChunk");
            Specification<KnowledgeChunk> specification = (root, query, cb) ->
                    null == knowledgeId || knowledgeId < 1 ? cb.conjunction() : cb.equal(root.get("knowledgeId"), knowledgeId);
            long total = chunkDao.count(specification);
            emitter.step("待重建检索块 " + total + " 个", "count", 10, (int) total);
            int pageSize = 500;
            long indexed = 0;
            long failed = 0;
            for (int page = 0; emitter.isRunning(); page++) {
                Page<KnowledgeChunk> data = chunkDao.findAll(specification,
                        PageRequest.of(page, pageSize, Sort.by(Sort.Order.asc("id"))));
                List<KnowledgeChunk> rows = data.getContent();
                if (rows.isEmpty()) break;
                Set<Integer> segmentIds = DPUtil.values(rows, Integer.class, "segmentId");
                Set<Integer> documentIds = DPUtil.values(rows, Integer.class, "documentId");
                Map<Integer, KnowledgeSegment> segments = DPUtil.list2map(segmentDao.findAllById(segmentIds), Integer.class, "id");
                Map<Integer, KnowledgeDocument> documents = DPUtil.list2map(documentDao.findAllById(documentIds), Integer.class, "id");
                ArrayNode sources = DPUtil.arrayNode();
                for (KnowledgeChunk chunk : rows) {
                    sources.add(chunkES.format(chunk, segments.get(chunk.getSegmentId()), documents.get(chunk.getDocumentId())));
                }
                List<String> result = chunkES.add(sources);
                long currentFailed = 0;
                if (null == result) {
                    currentFailed = sources.size();
                } else {
                    for (String id : result) {
                        if (null == id) currentFailed++;
                    }
                }
                failed += currentFailed;
                indexed += sources.size() - currentFailed;
                long current = Math.min((long) (page + 1) * pageSize, total);
                int progress = 10 + (int) Math.round(90.0 * current / Math.max(total, 1));
                emitter.log(String.format("已重建 %d/%d 个检索块，本次失败 %d 个", current, total, currentFailed),
                        "index", Math.min(progress, 100), currentFailed > 0 ? "warning" : "success", (int) current, (int) total);
                if (rows.size() < pageSize) break;
            }
            ObjectNode result = DPUtil.objectNode();
            result.put("total", total);
            result.put("indexed", indexed);
            result.put("failed", failed);
            emitter.log("检索块索引重建完成：成功 " + indexed + " 个，失败 " + failed + " 个", "index", 100,
                    failed > 0 ? "warning" : "success");
            emitter.result(0, "检索块索引重建完成", result);
        } catch (Exception e) {
            emitter.error(1500, "重建检索块索引失败：" + e.getMessage(), e.getMessage());
        } finally {
            reindexing.set(false);
        }
        return emitter;
    }

    /**
     * 删除检索块，先清理 Elasticsearch 记录，索引删除失败时不再删除数据库记录，便于重试
     */
    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        // 同步删除 Elasticsearch 记录
        long indexed = chunkES.delete(ids);
        if (ElasticsearchBase.STATE_FAILED == indexed) {
            return ApiUtil.result(1500, "删除检索索引失败", ids);
        }
        return remove(chunkDao, ids);
    }

}
