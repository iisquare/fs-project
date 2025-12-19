package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.KnowledgeDao;
import com.iisquare.fs.web.lm.entity.Knowledge;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeService extends JPAServiceBase {

    @Autowired
    private KnowledgeDao knowledgeDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    ModelService modelService;

    public Map<String, Object> embedding(Map<String, Object> param) {
        Knowledge knowledge = info(DPUtil.parseInt(param.get("knowledgeId")));
        if (null == knowledge) {
            return ApiUtil.result(1001, "请选择合适的知识库", null);
        }
        String content = DPUtil.parseString(param.get("content"));
        return modelService.embedding(knowledge.getEmbeddingId(), Collections.singletonList(content));
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> splitTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("chunk", "检索块");
        types.put("segment", "父子分段");
        types.put("document", "全文");
        return types;
    }

    public Knowledge info(Integer id) {
        return info(knowledgeDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "知识库名称异常", name);
        String splitType = DPUtil.parseString(param.get("splitType"));
        if (!splitTypes().containsKey(splitType)) return ApiUtil.result(1002, "拆分方式异常", splitType);
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
        info.setEmbeddingId(DPUtil.parseInt(param.get("embeddingId")));
        info.setRerankerId(DPUtil.parseInt(param.get("rerankerId")));
        info.setTopK(DPUtil.parseInt(param.get("topK")));
        info.setScore(DPUtil.parseFloat(param.get("score")));
        info.setSplitType(splitType);
        info.setSplitSeparator(DPUtil.parseString(param.get("splitSeparator")));
        info.setSplitSegmentTokens(DPUtil.parseInt(param.get("splitSegmentTokens")));
        info.setSplitChunkTokens(DPUtil.parseInt(param.get("splitChunkTokens")));
        info.setSplitOverlayTokens(DPUtil.parseInt(param.get("splitOverlayTokens")));
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
            DPUtil.fillValues(rows, "splitType", "splitTypeText", splitTypes());
            modelService.fillInfo(rows, "embeddingId", "rerankerId");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(knowledgeDao, ids);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(knowledgeDao, json, properties);
    }

}
