package com.iisquare.fs.web.oa.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.helper.FilterHelper;
import com.iisquare.fs.base.mongodb.util.MongoUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.oa.mongodb.FormDataMongo;
import com.iisquare.fs.web.oa.service.FormDataService;
import com.iisquare.fs.web.oa.service.FormService;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FormDefaultStorage extends FormStorage {

    @Autowired
    private FormDataMongo formDataMongo;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private FormService formService;
    @Autowired
    private FormDataService formDataService;

    @Override
    public Map<String, Object> search(ObjectNode frame, Map<String, Object> param, Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Bson sort = MongoUtil.sort(DPUtil.toJSON(param.get("sort")), null);
        if (null == sort) sort = Sorts.descending(MongoCore.FIELD_ID);
        FilterHelper helper = FilterHelper.newInstance(param);
        helper.addAll(condition2filters(
                DPUtil.toJSON(param.get("condition")),
                formService.fields(frame.at("/widgets"), false)));
        helper.add(Filters.eq("frameId", frame.at("/id").asInt(0)));
        Bson filter = helper.filter();
        long total = formDataMongo.count(filter);
        List<Document> rows = total > 0 ? formDataMongo.all(filter, sort, page, pageSize) : Arrays.asList();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

    @Override
    public ObjectNode save(ObjectNode frame, ObjectNode data, int uid) {
        Document document = MongoUtil.fromJson(data);
        document.put("frameId", frame.at("/id").asInt(0));
        document = formDataService.save(document, uid);
        return DPUtil.toJSON(document, ObjectNode.class);
    }

    @Override
    public long delete(ObjectNode frame, List<String> ids, int uid) {
        return formDataService.delete(ids, uid);
    }

    @Override
    public ObjectNode info(ObjectNode frame, String id) {
        return DPUtil.toJSON(formDataService.info(id), ObjectNode.class);
    }

    public List<Bson> condition2filters(JsonNode condition, JsonNode fields) {
        List<Bson> list = new ArrayList<>();
        if (null == condition || condition.isNull()) return list;
        Iterator<JsonNode> iterator = condition.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (item.has("enabled") && !item.get("enabled").asBoolean(false)) continue;
            String type = item.at("/type").asText();
            String field = item.at("/field").asText();
            String operation = item.at("/operation").asText("");
            String value = item.at("/value").asText();
            if ("RELATION".equals(type)) {
                List<Bson> children = condition2filters(item.at("/children"), fields);
                if (children.size() < 1) continue;
                if ("AND".equals(value)) {
                    list.add(Filters.and(children));
                    continue;
                }
                if ("OR".equals(value)) {
                    list.add(Filters.or(children));
                    continue;
                }
                continue;
            }
            if (!"FILTER".equals(type) || DPUtil.empty(field)) continue;
            Object val = convertValueType(fields.at("/" + field), value);
            switch (operation) {
                case "EQUAL":
                    list.add(Filters.eq(field, val));
                    break;
                case "NOT_EQUAL":
                    list.add(Filters.ne(field, val));
                case "LESS_THAN":
                    list.add(Filters.lt(field, val));
                case "LESS_THAN_OR_EQUAL":
                    list.add(Filters.lte(field, val));
                case "GREATER_THAN":
                    list.add(Filters.gt(field, val));
                case "GREATER_THAN_OR_EQUAL":
                    list.add(Filters.gte(field, val));
                case "IS_NULL":
                    list.add(Filters.eq(field, null));
                case "IS_NOT_NULL":
                    list.add(Filters.ne(field, null));
                case "LIKE":
                    list.add(Filters.regex(field, value));
                case "NOT_LIKE":
                    list.add(Filters.not(Filters.regex(field, value)));
                case "IN":
                    list.add(Filters.in(field, val));
                case "NOT_IN":
                    list.add(Filters.nin(field, val));
            }
        }
        return list;
    }

    public Object convertValueType(JsonNode item, String value) {
        String type = item.at("/type").asText("");
        if ("number".equals(type)) return DPUtil.parseDouble(value);
        if ("switch".equals(type)) return DPUtil.parseBoolean(value);
        return value;
    }

}
