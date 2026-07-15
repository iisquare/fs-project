package com.iisquare.fs.base.elasticsearch.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.*;

public class ElasticsearchUtil {

    public static final String FIELD_ID = "_id";
    public static final String FIELD_SCORE = "_score";
    public static final String FIELD_HIGHLIGHT = "_highlight";

    public static List<String> ids(HitsMetadata<?> hits) {
        List<String> ids = new ArrayList<>();
        if (null == hits) return ids;
        for (Hit<?> hit : hits.hits()) {
            ids.add(hit.id());
        }
        return ids;
    }

    public static long total(SearchResponse<ObjectNode> response) {
        if (null == response || null == response.hits() || null == response.hits().total()) {
            return -1;
        }
        return response.hits().total().value();
    }

    public static ObjectNode sources(SearchResponse<ObjectNode> response, boolean withExtend) {
        ObjectNode result = DPUtil.objectNode();
        if (null == response || null == response.hits() || null == response.hits().hits()) {
            return result;
        }
        for (Hit<ObjectNode> hit : response.hits().hits()) {
            ObjectNode source = hit.source();
            if (null == source) source = DPUtil.objectNode();
            result.replace(hit.id(), source);
            if (!withExtend) continue;
            source.put(FIELD_ID, hit.id());
            source.put(FIELD_SCORE, hit.score());
            Map<String, List<String>> highlight = hit.highlight();
            if (null != highlight && !highlight.isEmpty()) {
                source.putPOJO(FIELD_HIGHLIGHT, highlight);
            }
        }
        return result;
    }

    public static List<SortOptions> sort(String sort, Collection<String> fields) {
        List<SortOptions> options = new ArrayList<>();
        if (DPUtil.empty(sort)) return options;
        String[] sorts = DPUtil.explode(",", sort);
        for (String item : sorts) {
            String[] explode = DPUtil.explode("\\.", item);
            String order = explode[0];
            if (!fields.contains(order)) continue;
            String direction = explode.length > 1 ? explode[1].toLowerCase() : "asc";
            SortOrder so = "desc".equals(direction) ? SortOrder.Desc : SortOrder.Asc;
            options.add(SortOptions.of(s -> s.field(f -> f.field(order).order(so))));
        }
        return options;
    }

    public static List<SortOptions> sort(String sort, Collection<String> fields, boolean keepScore) {
        if (keepScore && !fields.contains(FIELD_SCORE)) fields.add(FIELD_SCORE);
        List<SortOptions> options = sort(sort, fields);
        if (keepScore) {
            options.add(SortOptions.of(s -> s.field(f -> f.field(FIELD_SCORE).order(SortOrder.Desc))));
        }
        return options;
    }

    public static Highlight highlight(Collection<String> highlights) {
        if (DPUtil.empty(highlights)) return null;
        Map<String, HighlightField> fieldMap = new LinkedHashMap<>();
        for (String highlight : highlights) {
            fieldMap.put(highlight, HighlightField.of(hf -> hf));
        }
        if (fieldMap.isEmpty()) return null;
        return Highlight.of(h -> h.fields(fieldMap));
    }

    public static RestClient rest(ElasticsearchClient client) {
        return ((RestClientTransport) client._transport()).restClient();
    }

    public static Response perform(ElasticsearchClient client, String endpoint, String method, String body) throws Exception {
        Request request = new Request(method, endpoint);
        request.setJsonEntity(body);
        return rest(client).performRequest(request);
    }

    public static String entity(Response response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

    public static List<Float> vector(JsonNode arr) {
        List<Float> vector = new ArrayList<>();
        for (JsonNode item : arr) {
            vector.add(item.floatValue());

        }
        return vector;
    }

}
