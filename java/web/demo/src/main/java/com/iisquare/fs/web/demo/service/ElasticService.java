package com.iisquare.fs.web.demo.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.util.ElasticsearchUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.demo.elasticsearch.DemoTestES;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticService extends ServiceBase {

    @Autowired
    private DemoTestES testES;

    public static final Map<String, String> highlights = new LinkedHashMap() {{
        put("name", "name");
        put("content", "content");
    }};

    public Map<?, ?> search(Map<String, Object> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        BoolQueryBuilder queries = QueryBuilders.boolQuery();
        List<QueryBuilder> must = queries.must();
        String id = DPUtil.trim(DPUtil.parseString(param.get("id")));
        if (!DPUtil.empty(id)) {
            must.add(QueryBuilders.termsQuery("id", Arrays.asList(DPUtil.explode(id, ","))));
        }
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (!DPUtil.empty(name)) {
            must.add(QueryBuilders.termsQuery("name", Arrays.asList((DPUtil.explode(name, ",")))));
        }
        SearchSourceBuilder search = SearchSourceBuilder.searchSource();
        search.query(queries);
        ElasticsearchUtil.order(search, DPUtil.parseString(param.get("sort")), Arrays.asList("_score"));
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, 100, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 5000, 1);
        search.from((page - 1) * pageSize).size(pageSize);
        String highlight = DPUtil.trim(DPUtil.parseString(param.get("highlight")));
        if (!DPUtil.empty(highlight)) {
            search.highlighter(ElasticsearchUtil.highlight(highlight, highlights, null));
        }
        search.fetchSource(true);
        SearchHits hits = testES.search(search);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", hits.getTotalHits().value);
        ObjectNode rows = format(hits);
        result.put("rows", DPUtil.empty(config.get("withRowsArray")) ? rows : DPUtil.arrayNode(rows));
        return result;
    }

    public ObjectNode format(SearchHits hits) {
        Map<String, Map<String, Object>> data = ElasticsearchUtil.mapWithId(hits);
        ObjectNode result = DPUtil.objectNode();
        if (data.size() < 1) return result;
        for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
            String id = entry.getKey();
            Map<String, Object> item = entry.getValue();
            ObjectNode node = DPUtil.objectNode();
            node.put("id", DPUtil.parseInt(item.get("id")));
            node.put("name", DPUtil.parseString(item.get("name")));
            node.put("content", DPUtil.parseString(item.get("content")));
            Object highlight = item.get("highlight");
            if (null != highlight) {
                node.replace("highlight", DPUtil.toJSON(highlight));
            }
            result.replace(id, node);
        }
        return result;
    }

}
