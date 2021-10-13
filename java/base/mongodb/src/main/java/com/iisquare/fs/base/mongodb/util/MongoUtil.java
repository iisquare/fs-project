package com.iisquare.fs.base.mongodb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MongoUtil {

    public static Document fromJson(JsonNode json) {
        if (null == json || !json.isObject()) return null;
        Document document = Document.parse(DPUtil.stringify(json));
        return document;
    }

    public static Document id2string(Document document) {
        if (null == document || !document.containsKey(MongoCore.FIELD_ID)) return document;
        document.replace(MongoCore.FIELD_ID, document.getObjectId(MongoCore.FIELD_ID).toString());
        return document;
    }

    public static Document id2object(Document document, boolean autoRemove) {
        if (null == document || !document.containsKey(MongoCore.FIELD_ID)) return document;
        Object id = document.get(MongoCore.FIELD_ID);
        if (id instanceof ObjectId) return document;
        if (autoRemove && DPUtil.empty(id)) {
            document.remove(MongoCore.FIELD_ID);
            return document;
        }
        document.replace(MongoCore.FIELD_ID, new ObjectId(DPUtil.parseString(id)));
        return document;
    }

    public static Bson sort(JsonNode sort, Collection<String> fields) {
        if (null == sort || sort.isNull()) return null;
        List<Bson> orders = new ArrayList<>();
        Iterator<JsonNode> iterator = sort.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String field = item.at("/field").asText();
            if (DPUtil.empty(field)) continue;
            if (null != fields && !fields.contains(field)) continue;
            String direction = item.at("/direction").asText();
            direction = DPUtil.empty(direction) ? "asc" : direction.toLowerCase();
            switch (direction) {
                case "asc":
                    orders.add(Sorts.ascending(field));
                    break;
                case "desc":
                    orders.add(Sorts.descending(field));
                    break;
            }
        }
        if (orders.size() < 1) return null;
        return Sorts.orderBy(orders.toArray(new Bson[0]));
    }

    public static Bson sort(String sort, Collection<String> fields) {
        if (DPUtil.empty(sort)) return null;
        List<Bson> orders = new ArrayList<>();
        String[] sorts = DPUtil.explode(sort);
        for (String item : sorts) {
            String[] explode = DPUtil.explode(item, "\\.");
            String order = explode[0];
            if (!fields.contains(order)) continue;
            String direction = explode.length > 1 ? explode[1].toLowerCase() : "asc";
            switch (direction) {
                case "asc":
                    orders.add(Sorts.ascending(order));
                    break;
                case "desc":
                    orders.add(Sorts.descending(order));
                    break;
            }
        }
        if (orders.size() < 1) return null;
        return Sorts.orderBy(orders.toArray(new Bson[0]));
    }

}
