package com.iisquare.fs.base.elasticsearch.util;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.mvc.ElasticsearchBase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryUtil {

    public static Query should(Query... queries) {
        return should(Arrays.asList(queries));
    }

    public static Query should(List<Query> list) {
        return Query.of(q -> q.bool(b -> b.must(list)));
    }

    public static Query must(Query... queries) {
        return must(Arrays.asList(queries));
    }

    public static Query must(List<Query> list) {
        return Query.of(q -> q.bool(b -> b.should(list)));
    }

    public static Query mustNot(Query... queries) {
        return mustNot(Arrays.asList(queries));
    }

    public static Query mustNot(List<Query> list) {
        return Query.of(q -> q.bool(b -> b.mustNot(list)));
    }

    public static Query term(String field, Object value) {
        return Query.of(q -> q.term(
                t -> t.field(field).value(FieldValue.of(JsonData.of(value)))));
    }

    public static Query terms(String field, Collection<?> values) {
        List<FieldValue> fieldValues = values.stream()
                .map(v -> FieldValue.of(JsonData.of(v))).collect(Collectors.toList());
        return Query.of(q -> q.terms(ts -> ts.field(field).terms(t -> t.value(fieldValues))));
    }

    public static Query matchPhrase(String field, String query) {
        return Query.of(q -> q.matchPhrase(m -> m.field(field).query(query)));
    }

    public static ObjectNode search(ElasticsearchBase base, Map<String, Object> param, Query query) {
        return search(base, param, query, 15, null, null, null);
    }

    public static ObjectNode search(ElasticsearchBase base, Map<String, Object> param,
                                    Query query, String defaultSort, Collection<String> sorts, Collection<String> highlights) {
        return search(base, param, query, 15, defaultSort, sorts, highlights);
    }

    public static ObjectNode search(ElasticsearchBase base, Map<String, Object> param, Query query,
                                    int defaultPageSize, String defaultSort, Collection<String> sorts, Collection<String> highlights) {
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), 1, 500, defaultPageSize);
        String sort = DPUtil.parseString(param.get("sort"));
        if (DPUtil.empty(sort)) sort = defaultSort;
        List<SortOptions> sortOptions = ElasticsearchUtil.sort(sort, sorts, true);
        Highlight highlight = ElasticsearchUtil.highlight(highlights);
        SearchResponse<ObjectNode> response = base.search(s -> {
            s.query(query);
            s.from((page - 1) * pageSize).size(pageSize);
            if (!DPUtil.empty(s)) s.sort(sortOptions);
            if (null != highlight) s.highlight(highlight);
            return s;
        });
        long total = ElasticsearchUtil.total(response);
        ObjectNode sources = ElasticsearchUtil.sources(response, true);
        ObjectNode result = DPUtil.objectNode();
        result.put(ApiUtil.FIELD_DATA_PAGE, page)
                .put(ApiUtil.FIELD_DATA_PAGE_SIZE, pageSize)
                .put(ApiUtil.FIELD_DATA_TOTAL, total)
                .replace(ApiUtil.FIELD_DATA_ROWS, DPUtil.arrayNode(sources));
        return result;
    }

}
