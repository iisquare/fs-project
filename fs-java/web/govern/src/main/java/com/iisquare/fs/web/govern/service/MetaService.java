package com.iisquare.fs.web.govern.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.elasticsearch.util.ElasticsearchUtil;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.govern.dao.ModelColumnDao;
import com.iisquare.fs.web.govern.dao.ModelDao;
import com.iisquare.fs.web.govern.dao.ModelRelationDao;
import com.iisquare.fs.web.govern.elasticsearch.MetaES;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.ModelRelation;
import com.iisquare.fs.web.govern.neo4j.MetaBloodRelation;
import com.iisquare.fs.web.govern.neo4j.MetaInfluenceRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetaService extends ServiceBase {

    @Autowired
    ModelDao modelDao;
    @Autowired
    ModelColumnDao columnDao;
    @Autowired
    ModelRelationDao relationDao;
    @Autowired
    MetaES metaES;
    @Autowired
    MetaBloodRelation metaBloodRelation;
    @Autowired
    MetaInfluenceRelation metaInfluenceRelation;
    @Autowired
    ModelService modelService;

    public static final List<String> highlights = List.of("code", "name", "description");

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

    public ObjectNode search(Map<String, Object> param, Map<?, ?> config) {
        Query query = Query.of(q -> {
            String prefix = DPUtil.trim(DPUtil.parseString(param.get("prefix")));
            if (!DPUtil.empty(prefix)) {
                q.prefix(p -> p.field("id").value(prefix));
            }
            String mold = DPUtil.trim(DPUtil.parseString(param.get("mold")));
            if (!DPUtil.empty(mold)) {
                q.term(t -> t.field("mold").value(mold));
            }
            String keyword = DPUtil.trim(DPUtil.parseString(param.get("keyword")));
            if (!DPUtil.empty(keyword)) {
                keyword = "*" + keyword + "*";
                String finalKeyword = keyword;
                q.bool(b -> b.should(s -> {
                    for (String highlight : highlights) {
                        s.wildcard(w -> w.field(highlight).value(finalKeyword));
                    }
                    return s;
                }));
            }
            return q;
        });
        ObjectNode result = QueryUtil.search(metaES, param, query, null, null, highlights);
        format(ApiUtil.rows(result));
        return result;
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

    public Map<String, Object> blood(Map<?, ?> param, Map<?, ?> config) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        int minHops = ValidateUtil.filterInteger(param.get("minHops"), 0, 10, 0);
        int maxHops = ValidateUtil.filterInteger(param.get("maxHops"), 0, 10, 1);
        ArrayNode blood = metaBloodRelation.blood(catalog, code, minHops, maxHops);
        ObjectNode result = Neo4jUtil.mergePath(blood, "p");
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> influence(Map<?, ?> param, Map<?, ?> config) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        String model = DPUtil.trim(DPUtil.parseString(param.get("model")));
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        int minHops = ValidateUtil.filterInteger(param.get("minHops"), 0, 10, 0);
        int maxHops = ValidateUtil.filterInteger(param.get("maxHops"), 0, 10, 1);
        ArrayNode blood = metaInfluenceRelation.influence(catalog, model, code, minHops, maxHops);
        ObjectNode result = Neo4jUtil.mergePath(blood, "p");
        Iterator<JsonNode> iterator = result.at("/nodes").iterator();
        Set<Model.IdClass> ids = new LinkedHashSet<>();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            ids.add(Model.IdClass.builder()
                    .catalog(node.at("/properties/catalog").asText())
                    .code(node.at("/properties/model").asText()).build());
        }
        result.replace("models", modelService.infos(ids));
        return ApiUtil.result(0, null, result);
    }

}
