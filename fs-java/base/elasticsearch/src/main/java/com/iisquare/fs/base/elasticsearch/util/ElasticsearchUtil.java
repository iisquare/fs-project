package com.iisquare.fs.base.elasticsearch.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;

public class ElasticsearchUtil {

    public static List<QueryBuilder> should(List<QueryBuilder> must) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        must.add(query);
        return query.should();
    }

    public static List<String> ids(SearchHits hits) {
        List<String> ids = new ArrayList<>();
        if (null == hits) return ids;
        for (SearchHit hit : hits.getHits()) {
            ids.add(hit.getId());
        }
        return ids;
    }

    public static Map<String, Map<String, Object>> mapWithId(SearchHits hits) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        if (null == hits) return map;
        for (SearchHit hit : hits.getHits()) {
            Map<String, Object> source = hit.getSourceAsMap();
            if (null == source) source = new LinkedHashMap<>();
            Map<String, List<String>> highlight = highlight(hit.getHighlightFields());
            if (highlight.size() > 0) source.put("highlight", highlight);
            map.put(hit.getId(), source);
        }
        return map;
    }

    public static Map<String, List<String>> highlight(Map<String, HighlightField> map) {
        if (null == map) return null;
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, HighlightField> entry : map.entrySet()) {
            List<String> list = new ArrayList<>();
            for (Text fragment : entry.getValue().fragments()) {
                list.add(fragment.string());
            }
            result.put(entry.getKey(), list);
        }
        return result;
    }

    public static <K> Map<K, Map<String, Object>> mapWithField(SearchHits hits, String field, Class<K> kType) {
        Map<K, Map<String, Object>> map = new LinkedHashMap<>();
        if (null == hits) return map;
        for (SearchHit hit : hits.getHits()) {
            map.put(hit.getFields().get(field).getValue(), hit.getSourceAsMap());
        }
        return map;
    }

    public static boolean order(SearchSourceBuilder search, String sort, Collection<String> fields) {
        if (DPUtil.empty(sort)) return false;
        String[] sorts = DPUtil.explode(",", sort);
        boolean result = false;
        for (String item : sorts) {
            String[] explode = DPUtil.explode("\\.", item);
            String order = explode[0];
            if (!fields.contains(order)) continue;
            String direction = explode.length > 1 ? explode[1].toLowerCase() : "asc";
            switch (direction) {
                case "asc":
                    search.sort(order, SortOrder.ASC);
                    result = true;
                    break;
                case "desc":
                    search.sort(order, SortOrder.DESC);
                    result = true;
                    break;
            }
        }
        return result;
    }

    public static HighlightBuilder highlight(String highlight, Map<String, String> highlights, List<String> sources) {
        HighlightBuilder highlighter = new HighlightBuilder();
        if (DPUtil.empty(highlight)) return highlighter;
        String[] strings =DPUtil.explode(",", highlight);
        for (String item : strings) {
            String field = highlights.get(item);
            if (null == field) continue;
            if (null != sources) sources.add(field);
            highlighter.field(field);
        }
        return highlighter;
    }

}
