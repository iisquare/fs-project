package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.KnowledgeChunkDao;
import com.iisquare.fs.web.lm.entity.KnowledgeChunk;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeChunkService extends JPAServiceBase {

    @Autowired
    private KnowledgeChunkDao chunkDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    KnowledgeService knowledgeService;
    @Autowired
    KnowledgeDocumentService documentService;

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
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
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
        info.setKnowledgeId(DPUtil.parseInt(param.get("knowledgeId")));
        info.setDocumentId(DPUtil.parseInt(param.get("documentId")));
        info.setSegmentId(DPUtil.parseInt(param.get("segmentId")));
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setEmbedding(DPUtil.parseString(param.get("embedding")));
        info.setStatus(status);
        info = save(chunkDao, info, rbacService.uid(request));
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
        return result;
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(chunkDao, ids);
    }

}
