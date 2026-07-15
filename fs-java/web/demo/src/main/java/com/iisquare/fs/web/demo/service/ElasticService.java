package com.iisquare.fs.web.demo.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.demo.elasticsearch.DemoTestES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ElasticService extends ServiceBase {

    @Autowired
    DemoTestES testES;

    public ObjectNode search(Map<String, Object> param, Map<?, ?> config) {
        List<Query> queries = new ArrayList<>();
        String id = DPUtil.trim(DPUtil.parseString(param.get("id")));
        if (!DPUtil.empty(id)) {
            queries.add(QueryUtil.term("id", id));
        }
        List<String> names = DPUtil.parseStringList(param.get("name"));
        if (!DPUtil.empty(names)) {
            queries.add(QueryUtil.term("name", names));
        }
        ObjectNode result = QueryUtil.search(testES, param,
                QueryUtil.must(queries),null, List.of("id", "name"), List.of("name", "content"));
        JsonNode rows = format(ApiUtil.rows(result));
        return result;
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

}
