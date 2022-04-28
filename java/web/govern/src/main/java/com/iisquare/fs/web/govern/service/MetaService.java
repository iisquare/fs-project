package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.util.ElasticsearchUtil;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.govern.dao.ModelColumnDao;
import com.iisquare.fs.web.govern.dao.ModelDao;
import com.iisquare.fs.web.govern.dao.ModelRelationDao;
import com.iisquare.fs.web.govern.elasticsearch.MetaES;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.ModelRelation;
import com.iisquare.fs.web.govern.neo4j.MetaRelation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetaService extends ServiceBase {

    @Autowired
    private ModelDao modelDao;
    @Autowired
    private ModelColumnDao columnDao;
    @Autowired
    private ModelRelationDao relationDao;
    @Autowired
    private MetaES metaES;
    @Autowired
    private MetaRelation metaRelation;

    public static final Map<String, String> highlights = new LinkedHashMap() {{
        put("code", "code");
        put("name", "name");
        put("description", "description");
    }};

    public ObjectNode statistic() {
        ObjectNode result = DPUtil.objectNode();
        result.put("modelCount", modelDao.count(
                (Specification<Model>) (root, query, cb) -> cb.notEqual(root.get("type"), "catalog")));
        result.put("relationCount", relationDao.count());
        result.put("relationModelCount", relationDao.count(
                (Specification<ModelRelation>) (root, query, cb) -> cb.equal(root.get("sourceColumn"), "")
        ));
        result.put("relationColumnCount", relationDao.count(
                (Specification<ModelRelation>) (root, query, cb) -> cb.notEqual(root.get("sourceColumn"), "")
        ));
        result.replace("relations", DPUtil.toJSON(relationDao.relations()));
        PageRequest pageable = PageRequest.of(0, 5, Sort.by(Sort.Order.desc("ct")));
        result.replace("types", DPUtil.toJSON(columnDao.types(pageable)));
        result.replace("modelSources", DPUtil.toJSON(relationDao.modelSources(pageable)));
        result.replace("columnSources", DPUtil.toJSON(relationDao.columnSources(pageable)));
        result.replace("modelTargets", DPUtil.toJSON(relationDao.modelTargets(pageable)));
        result.replace("columnTargets", DPUtil.toJSON(relationDao.columnTargets(pageable)));
        return result;
    }

    public Map<String, Object> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        BoolQueryBuilder queries = QueryBuilders.boolQuery();
        List<QueryBuilder> must = queries.must();
        String prefix = DPUtil.trim(DPUtil.parseString(param.get("prefix")));
        if (!DPUtil.empty(prefix)) {
            must.add(QueryBuilders.prefixQuery("id", prefix));
        }
        String mold = DPUtil.trim(DPUtil.parseString(param.get("mold")));
        if (!DPUtil.empty(mold)) {
            must.add(QueryBuilders.termsQuery("mold", Arrays.asList((DPUtil.explode(mold, ",")))));
        }
        String keyword = DPUtil.trim(DPUtil.parseString(param.get("keyword")));
        if (!DPUtil.empty(keyword)) {
            keyword = "*" + keyword + "*";
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            List<QueryBuilder> should = boolQuery.should();
            for (Map.Entry<String, String> entry : highlights.entrySet()) {
                should.add(QueryBuilders.wildcardQuery(entry.getKey(), keyword));
            }
            must.add(boolQuery);
        }
        SearchSourceBuilder search = SearchSourceBuilder.searchSource();
        search.query(queries);
        ElasticsearchUtil.order(search, DPUtil.parseString(param.get("sort")), Arrays.asList("_score"));
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, 100, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 5000, 15);
        search.from((page - 1) * pageSize).size(pageSize);
        String highlight = DPUtil.trim(DPUtil.parseString(param.get("highlight")));
        if (!DPUtil.empty(highlight)) {
            search.highlighter(ElasticsearchUtil.highlight(highlight, highlights, null));
        }
        search.fetchSource(true);
        SearchHits hits = metaES.search(search);
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
            node.put("id", DPUtil.parseString(item.get("id")));
            node.put("code", DPUtil.parseString(item.get("code")));
            node.put("name", DPUtil.parseString(item.get("name")));
            node.put("mold", DPUtil.parseString(item.get("mold")));
            node.put("description", DPUtil.parseString(item.get("description")));
            Object highlight = item.get("highlight");
            if (null != highlight) {
                node.replace("highlight", DPUtil.toJSON(highlight));
            }
            result.replace(id, node);
        }
        return result;
    }

    public Map<String, Object> blood(Map<?, ?> param, Map<?, ?> config) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        int minHops = ValidateUtil.filterInteger(param.get("minHops"), true, 0, 10, 0);
        int maxHops = ValidateUtil.filterInteger(param.get("maxHops"), true, 0, 10, 1);
        ArrayNode blood = metaRelation.blood(catalog, code, minHops, maxHops);
        ObjectNode result = Neo4jUtil.mergePath(blood, "p");
        return ApiUtil.result(0, null, result);
    }

}
