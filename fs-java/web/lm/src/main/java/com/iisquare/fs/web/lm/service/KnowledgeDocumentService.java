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
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FileRpc;
import com.iisquare.fs.web.lm.dao.KnowledgeChunkDao;
import com.iisquare.fs.web.lm.dao.KnowledgeDocumentDao;
import com.iisquare.fs.web.lm.dao.KnowledgeSegmentDao;
import com.iisquare.fs.web.lm.elasticsearch.KnowledgeChunkES;
import com.iisquare.fs.web.lm.entity.Knowledge;
import com.iisquare.fs.web.lm.entity.KnowledgeChunk;
import com.iisquare.fs.web.lm.entity.KnowledgeDocument;
import com.iisquare.fs.web.lm.entity.KnowledgeSegment;
import com.iisquare.fs.web.lm.mvc.Configuration;
import com.iisquare.fs.web.lm.tool.DocumentParser;
import com.iisquare.fs.web.lm.tool.TextSplitter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class KnowledgeDocumentService extends JPAServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeDocumentService.class);

    @Autowired
    KnowledgeDocumentDao documentDao;
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
    FileRpc fileRpc;
    @Autowired
    AIService aiService;
    @Autowired
    KnowledgeChunkES chunkES;

    public static final String bucket = "fs-lm-knowledge";

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public KnowledgeDocument info(Integer id) {
        return info(documentDao, id);
    }

    /**
     * 上传文档并解析为 Markdown，依据所属知识库的分段/分块策略写入数据库
     */
    @Transactional
    public Map<String, Object> upload(MultipartFile file, Map<?, ?> param, HttpServletRequest request) {
        if (null == file || file.isEmpty()) return ApiUtil.result(1001, "获取文件句柄失败", null);
        int uid = rbacService.uid(request);
        Integer knowledgeId = ValidateUtil.filterInteger(param.get("knowledgeId"), true, 1, null, 0);
        if (null == knowledgeId || knowledgeId <= 0) return ApiUtil.result(1002, "请选择合适的知识库", knowledgeId);
        Knowledge knowledge = knowledgeService.info(knowledgeId);
        if (null == knowledge) return ApiUtil.result(1002, "知识库不存在", knowledgeId);
        if (!rbacService.hasPermit(request, "knowledge", "add")) return ApiUtil.result(9403, null, null);
        String filename = file.getOriginalFilename();
        if (!DocumentParser.supported(filename)) {
            return ApiUtil.result(1003, "不支持的文件类型", DocumentParser.suffix(filename));
        }
        // 解析为 Markdown
        String markdown;
        try {
            markdown = DocumentParser.parse(filename, file.getInputStream());
        } catch (Exception e) {
            logger.warn("parse document failed: {} - {}", filename, e.getMessage());
            return ApiUtil.result(1502, "解析文件失败", e.getMessage());
        }
        if (DPUtil.empty(markdown)) markdown = "";
        // 写入文档记录
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String suffix = DocumentParser.suffix(filename);
        String name = UUID.randomUUID().toString().replace("-", "") + (suffix.isEmpty() ? "" : "." + suffix);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setName(filename);
        document.setKnowledgeId(knowledgeId);
        document.setFilepath(String.format("knowledge-%d/%s/%s", knowledgeId, date, name));
        document.setTokenSize(markdown.length());
        document.setMetadata(DPUtil.stringify(DPUtil.objectNode()));
        document.setStatus(1);
        document = save(documentDao, document, uid);
        // 按召回范围分流写入分段/分块
        String scope = DPUtil.parseString(knowledge.getRecallScope());
        List<String> segmentContents = "document".equals(scope) ? List.of(markdown) : TextSplitter.splitSegments(
                markdown, knowledge.getSplitSeparator(), DPUtil.parseInt(knowledge.getSplitSegmentTokens()));
        int chunkCount = 0;
        long time = System.currentTimeMillis();
        int segmentCount = segmentContents.size();
        for (String segmentContent : segmentContents) {
            segmentCount++;
            KnowledgeSegment segment = KnowledgeSegment.builder()
                    .knowledgeId(document.getKnowledgeId()).documentId(document.getId())
                    .content(segmentContent).tokenSize(segmentContent.length()).status(1).build();
            segment = save(segmentDao, segment, uid);
            List<String> chunkContents = TextSplitter.splitChunks(segmentContent,
                    DPUtil.parseInt(knowledge.getSplitChunkTokens()), DPUtil.parseInt(knowledge.getSplitOverlayTokens()));
            JsonNode embeddings = DPUtil.arrayNode();
            if (!DPUtil.empty(knowledge.getEmbeddingModel())) {
                Map<String, Object> result = aiService.embeddings(knowledge.getEmbeddingModel(), chunkContents);
                if (ApiUtil.failed(result)) return result;
                embeddings = ApiUtil.data(result, ObjectNode.class).at("/data");
            }
            chunkCount += chunkContents.size();
            List<KnowledgeChunk> chunks = new ArrayList<>();
            for (int index = 0; index < chunkContents.size(); index++) {
                KnowledgeChunk chunk = KnowledgeChunk.builder()
                        .knowledgeId(segment.getKnowledgeId()).documentId(document.getId())
                        .segmentId(segment.getId()).content(chunkContents.get(index)).embedding("")
                        .createdUid(uid).createdTime(time).updatedUid(uid).updatedTime(time).status(1).build();
                if (!embeddings.isEmpty()) {
                    chunk.setEmbedding(DPUtil.stringify(embeddings.get(index).at("/embedding")));
                }
                chunks.add(chunk);
            }
            chunks = chunkDao.saveAll(chunks);
            // 同步写入 Elasticsearch，冗余文档标题和元数据
            ArrayNode array = DPUtil.arrayNode();
            for (KnowledgeChunk chunk : chunks) {
                array.add(chunkES.format(chunk, document));
            }
            chunkES.add(array);
        }
        Map<String, Object> result = RpcUtil.result(fileRpc.form("/file/upload", DPUtil.buildMap(
                "bucket", bucket, "filepath", document.getFilepath(),
                "traceIdentity", String.format("fs-lm-knowledge-%d-document-%d", knowledgeId, document.getId())
        ), file));
        if (ApiUtil.failed(result)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return result;
        } else {
            document.setFileId(ApiUtil.data(result, ObjectNode.class).at("/id").asText());
            document = save(documentDao, document, uid);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", document.getId());
        data.put("name", document.getName());
        data.put("knowledgeId", document.getKnowledgeId());
        data.put("fileId", document.getFileId());
        data.put("filepath", document.getFilepath());
        data.put("tokenSize", document.getTokenSize());
        data.put("segmentCount", segmentCount);
        data.put("chunkCount", chunkCount);
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> download(Map<String, Object> param) {
        KnowledgeDocument info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", param);
        String fileId = info.getFileId();
        if (DPUtil.empty(fileId)) return ApiUtil.result(1401, "文件不存在", param);
        return RpcUtil.result(fileRpc.get("/file/download", DPUtil.buildMap("id", fileId)));
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "知识库名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        String metadata = DPUtil.stringify(param.get("metadata"));
        if (DPUtil.empty(metadata)) return ApiUtil.result(1004, "元数据异常", metadata);
        KnowledgeDocument info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "knowledge", "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "knowledge", "add")) return ApiUtil.result(9403, null, null);
            info = new KnowledgeDocument();
        }
        info.setName(name);
        info.setKnowledgeId(DPUtil.parseInt(param.get("knowledgeId")));
        info.setFilepath(DPUtil.parseString(param.get("filepath")));
        info.setTokenSize(DPUtil.parseInt(param.get("tokenSize")));
        info.setMetadata(metadata);
        info.setStatus(status);
        info = save(documentDao, info, rbacService.uid(request));
        chunkES.updateDocument(info);
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(documentDao, param, (root, query, cb) -> {
            SpecificationHelper<KnowledgeDocument> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("knowledgeId");
            helper.equalWithIntNotEmpty("status").like("name").like("metadata");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if(!DPUtil.empty(args.get("withKnowledgeInfo"))) {
            knowledgeService.fillInfo(rows, "knowledgeId");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("metadata", DPUtil.parseJSON(node.at("/metadata").asText(), k -> DPUtil.objectNode()));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        ArrayNode args = DPUtil.arrayNode();
        for (KnowledgeDocument document : documentDao.findAllById(ids)) {
            ObjectNode item = args.addObject();
            item.put("id", document.getId());
            item.put("bucket", bucket);
            item.put("filepath", document.getFilepath());
        }
        Map<String, Object> result = RpcUtil.result(fileRpc.post("/file/delete", args));
        if (ApiUtil.failed(result)) return false;
        if (!ids.isEmpty()) {
            chunkES.deleteByQuery(QueryUtil.terms("document_id", ids));
        }
        removeByParentId(segmentDao, "documentId", ids);
        removeByParentId(chunkDao, "documentId", ids);
        return remove(documentDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(documentDao, rows, properties);
    }

}
