package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.dao.ModelRelationDao;
import com.iisquare.fs.web.govern.entity.ModelRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class ModelRelationService extends ServiceBase {

    @Autowired
    private ModelRelationDao relationDao;
    @Autowired
    private DefaultRbacService rbacService;

    public ObjectNode relations() {
        ObjectNode result = DPUtil.objectNode();
        result.putObject("inherit").put("name", "继承").put("level", "model");
        result.putObject("depend").put("name", "依赖").put("level", "model");
        result.putObject("association").put("name", "关联").put("level", "model");
        result.putObject("aggregation").put("name", "聚合").put("level", "model");
        result.putObject("composition").put("name", "组合").put("level", "model");
        result.putObject("foreign").put("name", "外键").put("level", "column");
        result.putObject("reference").put("name", "参照").put("level", "column");
        return result;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")),
                Arrays.asList("sourceCatalog", "sourceModel", "sourceColumn", "targetCatalog", "targetModel", "targetColumn"));
        if (null == sort) sort = Sort.by(Sort.Order.asc("sourceCatalog"), Sort.Order.desc("sourceModel"), Sort.Order.desc("sourceColumn"));
        Page<ModelRelation> data = relationDao.findAll((Specification<ModelRelation>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> sources = new ArrayList<>();
            List<Predicate> targets = new ArrayList<>();
            String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
            if(!DPUtil.empty(catalog)) {
                sources.add(cb.like(root.get("sourceCatalog"), catalog));
                targets.add(cb.like(root.get("targetCatalog"), catalog));
            }
            String model = DPUtil.trim(DPUtil.parseString(param.get("model")));
            if(!DPUtil.empty(model)) {
                sources.add(cb.like(root.get("sourceModel"), model));
                targets.add(cb.like(root.get("targetModel"), model));
            }
            String column = DPUtil.trim(DPUtil.parseString(param.get("column")));
            if(!DPUtil.empty(column)) {
                sources.add(cb.like(root.get("sourceColumn"), column));
                targets.add(cb.like(root.get("targetColumn"), column));
            }
            if (sources.size() > 0) {
                predicates.add(cb.or(
                        cb.and(sources.toArray(new Predicate[0])), cb.and(targets.toArray(new Predicate[0]))));
            }
            String sourceCatalog = DPUtil.trim(DPUtil.parseString(param.get("sourceCatalog")));
            if(!DPUtil.empty(sourceCatalog)) {
                predicates.add(cb.like(root.get("sourceCatalog"), sourceCatalog));
            }
            String sourceModel = DPUtil.trim(DPUtil.parseString(param.get("sourceModel")));
            if(!DPUtil.empty(sourceModel)) {
                predicates.add(cb.like(root.get("sourceModel"), sourceModel));
            }
            String sourceColumn = DPUtil.trim(DPUtil.parseString(param.get("sourceColumn")));
            if(!DPUtil.empty(sourceColumn)) {
                predicates.add(cb.like(root.get("sourceColumn"), sourceColumn));
            }
            String relation = DPUtil.trim(DPUtil.parseString(param.get("relation")));
            if(!DPUtil.empty(relation)) {
                predicates.add(cb.equal(root.get("relation"), relation));
            }
            String targetCatalog = DPUtil.trim(DPUtil.parseString(param.get("targetCatalog")));
            if(!DPUtil.empty(targetCatalog)) {
                predicates.add(cb.like(root.get("targetCatalog"), targetCatalog));
            }
            String targetModel = DPUtil.trim(DPUtil.parseString(param.get("targetModel")));
            if(!DPUtil.empty(targetModel)) {
                predicates.add(cb.like(root.get("targetModel"), targetModel));
            }
            String targetColumn = DPUtil.trim(DPUtil.parseString(param.get("targetColumn")));
            if(!DPUtil.empty(targetColumn)) {
                predicates.add(cb.like(root.get("targetColumn"), targetColumn));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, sort));
        List<ModelRelation> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public ModelRelation info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<ModelRelation> info = relationDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String sourceCatalog = DPUtil.trim(DPUtil.parseString(param.get("sourceCatalog")));
        String sourceModel = DPUtil.trim(DPUtil.parseString(param.get("sourceModel")));
        String sourceColumn = DPUtil.trim(DPUtil.parseString(param.get("sourceColumn")));
        String relation = DPUtil.trim(DPUtil.parseString(param.get("relation")));
        String targetCatalog = DPUtil.trim(DPUtil.parseString(param.get("targetCatalog")));
        String targetModel = DPUtil.trim(DPUtil.parseString(param.get("targetModel")));
        String targetColumn = DPUtil.trim(DPUtil.parseString(param.get("targetColumn")));
        if (DPUtil.empty(sourceCatalog)) {
            return ApiUtil.result(1401, "源所属包不能为空", sourceCatalog);
        }
        if (DPUtil.empty(sourceModel)) {
            return ApiUtil.result(1402, "源模型名称不能为空", sourceModel);
        }
        if (DPUtil.empty(targetCatalog)) {
            return ApiUtil.result(1403, "目标所属包不能为空", targetCatalog);
        }
        if (DPUtil.empty(targetModel)) {
            return ApiUtil.result(1404, "目标模型名称不能为空", targetModel);
        }
        ObjectNode relations = relations();
        if (!relations.has(relation)) {
            return ApiUtil.result(1405, "模型关系类型暂不支持", relation);
        }
        String level = relations.get(relation).get("level").asText();
        if ("column".equals(level) && (DPUtil.empty(sourceColumn) || DPUtil.empty(targetColumn))) {
            return ApiUtil.result(1406, "仅模型字段可设置外键关系", relation);
        }
        if (!"column".equals(level) && (!DPUtil.empty(sourceColumn) || !DPUtil.empty(targetColumn))) {
            return ApiUtil.result(1407, "模型字段仅可设置外键关系", relation);
        }
        if ((DPUtil.empty(sourceColumn) && !DPUtil.empty(targetColumn))
                || (!DPUtil.empty(sourceColumn) && DPUtil.empty(targetColumn))) {
            return ApiUtil.result(1408, "字段不可与模型直接建立关系", param);
        }
        long time = System.currentTimeMillis();
        int uid = rbacService.uid(request);
        ModelRelation info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new ModelRelation();
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        info.setSourceCatalog(sourceCatalog);
        info.setSourceModel(sourceModel);
        info.setSourceColumn(sourceColumn);
        info.setRelation(relation);
        info.setTargetCatalog(targetCatalog);
        info.setTargetModel(targetModel);
        info.setTargetColumn(targetColumn);
        if (null == info.getId() || param.containsKey("description")) {
            info.setDescription(DPUtil.parseString(param.get("description")));
        }
        relationDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList(param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        if(ids.size() < 1) return ApiUtil.result(0, null, ids.size());
        List<ModelRelation> list = relationDao.findAllById(ids);
        if (list.size() > 0) relationDao.deleteInBatch(list);
        return ApiUtil.result(0, null, list.size());
    }

}
