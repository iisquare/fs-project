package com.iisquare.fs.base.mongodb.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.mvc.MongoBase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

public class FindUtil {

    public static Bson should(Bson... filters) {
        return should(Arrays.asList(filters));
    }

    public static Bson should(List<Bson> list) {
        return Filters.and(list);
    }

    public static Bson must(Bson... filters) {
        return must(Arrays.asList(filters));
    }

    public static Bson must(List<Bson> list) {
        return Filters.or(list);
    }

    public static Bson mustNot(Bson... filters) {
        return mustNot(Arrays.asList(filters));
    }

    public static Bson mustNot(List<Bson> list) {
        return Filters.nor(list);
    }

    public static Bson eq(String field, Object value) {
        return Filters.eq(field, value);
    }

    public static Bson in(String field, Collection<?> values) {
        return Filters.in(field, values);
    }

    public static Bson regex(String field, String pattern) {
        return Filters.regex(field, pattern);
    }

    public static ObjectNode search(MongoBase base, Map<String, Object> param, Bson filter) {
        return search(base, param, filter, 15, null, null);
    }

    public static ObjectNode search(MongoBase base, Map<String, Object> param,
                                    Bson filter, String defaultSort, Collection<String> sorts) {
        return search(base, param, filter, 15, defaultSort, sorts);
    }

    public static ObjectNode search(MongoBase base, Map<String, Object> param, Bson filter,
                                    int defaultPageSize, String defaultSort, Collection<String> sorts) {
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), 1, 500, defaultPageSize);
        String sort = DPUtil.parseString(param.get("sort"));
        if (DPUtil.empty(sort)) sort = defaultSort;
        Bson sortBson = MongoUtil.sort(sort, sorts);
        if (null == sortBson) sortBson = Sorts.descending(MongoCore.FIELD_ID);
        long total = base.count(filter);
        List<Document> rows = total > 0 ? base.all(filter, sortBson, page, pageSize) : new ArrayList<>();
        ObjectNode result = DPUtil.objectNode();
        result.put(ApiUtil.FIELD_DATA_PAGE, page)
                .put(ApiUtil.FIELD_DATA_PAGE_SIZE, pageSize)
                .put(ApiUtil.FIELD_DATA_TOTAL, total);
        result.putPOJO(ApiUtil.FIELD_DATA_ROWS, rows);
        return result;
    }

}
