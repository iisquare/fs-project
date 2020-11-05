package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.TemplateDao;
import com.iisquare.fs.web.spider.entity.Template;
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
public class TemplateService extends ServiceBase {

    @Autowired
    private TemplateDao templateDao;
    @Autowired
    private DefaultRbacService rbacService;

    public ObjectNode output(ObjectNode item) {
        if (item.has("outputType")) {
            ObjectNode output = DPUtil.objectNode();
            output.put("type", item.get("outputType").asText());
            output.replace("property", item.get("outputProperty"));
            item.put("output", DPUtil.stringify(output));
        }
        item.remove(Arrays.asList("page", "outputType", "outputProperty"));
        return item;
    }

    public JsonNode plain(Integer id) {
        Template info = info(id);
        if (null == info) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("group", info.getType());
        result.put("description", info.getDescription());
        JsonNode content = DPUtil.parseJSON(info.getContent());
        if (null == content) return null;
        result.replace("priority", content.get("priority"));
        result.replace("maxThread", content.get("maxThread"));
        result.replace("maxPerNode", content.get("maxPerNode"));
        result.replace("minHalt", content.get("minHalt"));
        result.replace("maxHalt", content.get("maxHalt"));
        result.replace("dealRequestHeader", content.get("dealRequestHeader"));
        result.replace("dealResponseHeader", content.get("dealResponseHeader"));
        ObjectNode templates = result.putObject("templates");
        Iterator<JsonNode> iterator = content.get("templates").elements();
        while (iterator.hasNext()) {
            Iterator<Map.Entry<String, JsonNode>> fields = iterator.next().fields();
            ObjectNode item = DPUtil.objectNode();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isTextual() && "".equals(value.asText())) continue;
                switch (key) {
                    case "headers":
                    case "outputProperty":
                        item.replace(key, DPUtil.parseJSON(value.asText()));
                        break;
                    default:
                        item.replace(key, value);
                }
            }
            templates.replace(item.get("page").asText(), output(item));
        }
        ArrayNode intercepts = result.putArray("intercepts");
        iterator = content.get("intercepts").elements();
        while (iterator.hasNext()) {
            Iterator<Map.Entry<String, JsonNode>> fields = iterator.next().fields();
            ObjectNode item = DPUtil.objectNode();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isTextual() && "".equals(value.asText())) continue;
                switch (key) {
                    case "outputProperty":
                        item.replace(key, DPUtil.parseJSON(value.asText()));
                        break;
                    default:
                        item.replace(key, value);
                }
            }
            intercepts.add(output(item));
        }
        result.put("initTask", content.get("initTask").asText());
        result.replace("initParams", DPUtil.parseJSON(content.get("initParams").asText()));
        return result;
    }

    public Template info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Template> info = templateDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Template save(Template info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        return templateDao.save(info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<?> data = templateDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
                if(!DPUtil.empty(type)) {
                    predicates.add(cb.equal(root.get("type"), type));
                }
                String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
                if(!DPUtil.empty(content)) {
                    predicates.add(cb.like(root.get("content"), "%" + content + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean delete(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        templateDao.deleteInBatch(templateDao.findAllById(ids));
        return true;
    }

}
