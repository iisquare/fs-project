package com.iisquare.fs.web.flink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.flink.dao.AnalysisNodeDao;
import com.iisquare.fs.web.flink.entity.AnalysisNode;
import com.iisquare.fs.web.flink.entity.FlowNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
public class AnalysisNodeService extends ServiceBase {

    @Autowired
    private AnalysisNodeDao analysisNodeDao;
    @Autowired
    private DefaultRbacService rbacService;

    public ArrayNode property(String content) {
        JsonNode node = DPUtil.parseJSON(content);
        if(null == node || !node.has("nodes")) return DPUtil.arrayNode();
        Iterator<JsonNode> fields = node.get("nodes").elements();
        while (fields.hasNext()) {
            ObjectNode property = DPUtil.objectNode();
            Iterator<JsonNode> iterator = fields.next().get("property").elements();
            while (iterator.hasNext()) {
                JsonNode prop = iterator.next();
                property.put(prop.get("key").asText(), prop.get("value").asText());
            }
            if(!property.get("classname").asText().endsWith(".plugins.olap.node.AnchorNode")) continue;
            return (ArrayNode) DPUtil.parseJSON(property.get("returns").asText());
        }
        return DPUtil.arrayNode();
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "关闭");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public AnalysisNode info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<AnalysisNode> info = analysisNodeDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public AnalysisNode save(AnalysisNode info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return analysisNodeDao.save(info);
    }

    public List<?> fillInfo(List<?> list, String ...properties) {
        if(null == list || list.size() < 1 || properties.length < 1) return list;
        Set<Integer> ids = ServiceUtil.getPropertyValues(list, Integer.class, properties);
        if(ids.size() < 1) return list;
        Map<Integer, AnalysisNode> map = ServiceUtil.indexObjectList(analysisNodeDao.findAllById(ids), Integer.class, AnalysisNode.class, "id");
        if(map.size() < 1) return list;
        for (Object item : list) {
            for (String property : properties) {
                AnalysisNode info = map.get(ReflectUtil.getPropertyValue(item, property));
                if(null == info) continue;
                ReflectUtil.setPropertyValue(item, property + "Name", null, new Object[]{info.getName()});
            }
        }
        return list;
    }

    public List<FlowNode> tree(Map<?, ?> param, Map<?, ?> args) {
        List<FlowNode> data = analysisNodeDao.findAll((Specification) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int status = DPUtil.parseInt(param.get("status"));
            if(!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), -1));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        }, Sort.by(Sort.Order.desc("sort")));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(data, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            ServiceUtil.fillProperties(data, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        return ServiceUtil.formatRelation(data, FlowNode.class, "parentId", 0, "id", "children");
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<?> data = analysisNodeDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.notEqual(root.get("status"), -1));
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                int parentId = DPUtil.parseInt(param.get("parentId"));
                if(!"".equals(DPUtil.parseString(param.get("parentId")))) {
                    predicates.add(cb.equal(root.get("parentId"), parentId));
                }
                String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
                if(!DPUtil.empty(type)) {
                    predicates.add(cb.equal(root.get("type"), type));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            ServiceUtil.fillProperties(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(config.get("withParentInfo"))) {
            this.fillInfo(rows, "parentId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        analysisNodeDao.deleteInBatch(analysisNodeDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<AnalysisNode> list = analysisNodeDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (AnalysisNode item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        analysisNodeDao.saveAll(list);
        return true;
    }
}
