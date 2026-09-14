package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.kg.dao.OntologyDao;
import com.iisquare.fs.web.kg.entity.Ontology;
import com.iisquare.fs.web.kg.mvc.Configuration;
import com.iisquare.fs.web.kg.ontology.OntologyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OntologyService extends JPAServiceBase {

    @Autowired
    OntologyDao ontologyDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    OntologyDefinitionService ontologyDefinitionService;

    /**
     * 规范化定义缓存，以本体的更新时间作为版本标记，保存时失效
     */
    private final Map<Integer, CachedModel> modelCache = new ConcurrentHashMap<>();

    protected static class CachedModel {
        public final long stamp;
        public final OntologyModel model;

        public CachedModel(long stamp, OntologyModel model) {
            this.stamp = stamp;
            this.model = model;
        }
    }

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        sorts.put("version", "desc");
        sorts.put("entityCount", "desc");
        sorts.put("relationshipCount", "desc");
        sorts.put("issueCount", "desc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Ontology info(Integer id) {
        return info(ontologyDao, id);
    }

    /**
     * 全部本体，用于全库巡检
     */
    public List<Ontology> all() {
        return ontologyDao.findAll(Sort.by(Sort.Order.asc("id")));
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Ontology info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "本体信息不存在", null);
        JsonNode node = DPUtil.firstNode(format(DPUtil.toArrayNode(info)));
        return ApiUtil.result(0, null, node);
    }

    /**
     * 读取本体定义模型
     *
     * 定义以规范化表为准，首次访问时按content自动迁移；结果按本体更新时间缓存。
     */
    public OntologyModel model(int id) {
        Ontology info = info(id);
        if (null == info) return null;
        long stamp = DPUtil.parseLong(info.getUpdatedTime());
        CachedModel cached = modelCache.get(id);
        if (null != cached && cached.stamp == stamp) return cached.model;
        OntologyModel model = ontologyDefinitionService.load(id);
        if (model.getEntities().isEmpty() && model.getRelationships().isEmpty()) {
            OntologyModel parsed = OntologyModel.parse(DPUtil.parseJSON(info.getContent()));
            if (!parsed.getEntities().isEmpty() || !parsed.getRelationships().isEmpty()) {
                // 历史数据首次访问时迁移到规范化存储
                ontologyDefinitionService.save(id, parsed, 0);
                model = ontologyDefinitionService.load(id);
                stamp = DPUtil.parseLong(info(id).getUpdatedTime());
            } else {
                model = parsed;
            }
        }
        modelCache.put(id, new CachedModel(stamp, model));
        return model;
    }

    /**
     * 获取本体模型，供图数据管理使用
     */
    public Map<String, Object> model(Map<?, ?> param) {
        int id = DPUtil.parseInt(param.get("id"));
        OntologyModel model = model(id);
        if (null == model) return ApiUtil.result(1404, "本体信息不存在", null);
        ObjectNode data = model.toJson();
        Ontology info = info(id);
        data.put("id", id);
        data.put("name", null == info ? "" : info.getName());
        data.put("version", null == info ? 0 : DPUtil.parseInt(info.getVersion()));
        data.put("updatedTime", null == info ? 0L : DPUtil.parseLong(info.getUpdatedTime()));
        return ApiUtil.result(0, null, data);
    }

    @Transactional
    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "本体名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Ontology info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
            int version = DPUtil.parseInt(param.get("version"));
            if (version > 0 && version != DPUtil.parseInt(info.getVersion())) {
                return ApiUtil.result(1007, "本体定义已被修改，请刷新后重试",
                        DPUtil.buildMap("version", DPUtil.parseInt(info.getVersion())));
            }
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Ontology();
        }
        JsonNode json = DPUtil.toJSON(param.get("content"));
        OntologyModel model = OntologyModel.parse(json);
        if (!json.has("cells") && (json.has("entities") || json.has("relationships"))) {
            // 结构化定义统一转换为画布格式存储，便于设计器直接打开
            json = model.toCanvasJson();
        }
        info.setName(name);
        info.setEntityCount(model.getEntities().size());
        info.setRelationshipCount(model.getRelationships().size());
        info.setContent(DPUtil.stringify(json));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info.setVersion(DPUtil.parseInt(info.getVersion()) + 1);
        info.setDefinitionTime(System.currentTimeMillis());
        info.setIssueCount(model.getIssues().size());
        info = save(ontologyDao, info, rbacService.uid(request));
        ontologyDefinitionService.save(info.getId(), model, rbacService.uid(request));
        modelCache.remove(info.getId());
        Map<String, Object> result = info(DPUtil.buildMap("id", info.getId()));
        JsonNode data = (JsonNode) result.get(ApiUtil.FIELD_DATA);
        if (data instanceof ObjectNode) {
            ((ObjectNode) data).set("issues", DPUtil.toJSON(model.getIssues()));
        }
        return result;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(ontologyDao, param, (root, query, cb) -> {
            SpecificationHelper<Ontology> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("content").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            String content = row.at("/content").asText("{}");
            node.replace("content", DPUtil.parseJSON(content));
        }
        return rows;
    }

    @Transactional
    public boolean remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return false;
        for (Integer id : ids) {
            ontologyDefinitionService.remove(id);
            modelCache.remove(id);
        }
        return remove(ontologyDao, ids);
    }


}
