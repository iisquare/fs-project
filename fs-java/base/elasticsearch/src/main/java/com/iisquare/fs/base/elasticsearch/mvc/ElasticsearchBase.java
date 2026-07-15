package com.iisquare.fs.base.elasticsearch.mvc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.RequestBase;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.get.GetResult;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.util.ElasticsearchUtil;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.StringReader;
import java.util.*;
import java.util.function.Function;

@Slf4j
public abstract class ElasticsearchBase {

    public static final int STATE_FAILED = -1;

    @Autowired
    protected ElasticsearchClient client;

    protected String collection = "fs_not_exists";
    protected int shards = 1;
    protected int replicas = 0;

    public String collection() {
        return this.collection;
    }

    public SearchResponse<ObjectNode> search(Function<SearchRequest.Builder, SearchRequest.Builder> fn) {
        return search(fn.apply(new SearchRequest.Builder()));
    }

    public SearchResponse<ObjectNode> search(SearchRequest.Builder builder) {
        SearchRequest request = builder.index(collection()).build();
        try {
            return client.search(request, ObjectNode.class);
        } catch (Exception e) {
            log.warn("search failed:{}", request, e);
            return null;
        }
    }

    public boolean update(ObjectNode source, boolean shouldUpsertDoc) {
        try {
            UpdateResponse<ObjectNode> response = client.update(u -> u
                    .index(collection()).id(id(source))
                    .doc(source).docAsUpsert(shouldUpsertDoc),
                    ObjectNode.class);
            Result result = response.result();
            return Result.Created.equals(result) || Result.Updated.equals(result) || Result.NoOp.equals(result);
        } catch (Exception e) {
            log.warn("update source failed:{}", source, e);
            return false;
        }
    }

    public abstract String id(ObjectNode source);

    public long delete(String id) {
        try {
            DeleteResponse response = client.delete(d -> d.index(collection()).id(id));
            return Result.Deleted.equals(response.result()) ? 1 : 0;
        } catch (Exception e) {
            log.warn("delete source failed:{}", id, e);
            return STATE_FAILED;
        }
    }

    public long delete(String... ids) {
        return delete(Arrays.asList(ids));
    }

    public long delete(Collection<?> ids) {
        if (DPUtil.empty(ids)) return 0;
        return deleteByQuery(QueryUtil.terms(ElasticsearchUtil.FIELD_ID, ids));
    }

    public long deleteByQuery(Query query) {
        try {
            DeleteByQueryResponse response = client.deleteByQuery(d -> d.index(collection()).query(query));
            Long total = response.total();
            return total == null ? 0 : total;
        } catch (Exception e) {
            log.warn("delete sources failed:{}", query, e);
            return STATE_FAILED;
        }
    }

    public long updateByQuery(ObjectNode source, Query query) {
        StringBuilder code = new StringBuilder();
        Map<String, JsonData> params = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : source.properties()) {
            String key = entry.getKey();
            code.append("ctx._source.").append(key).append(" = params.").append(key).append(";");
            params.put(key, JsonData.of(entry.getValue()));
        }
        Script script = Script.of(s -> s.lang("painless").source(code.toString()).params(params));
        try {
            UpdateByQueryResponse response = client.updateByQuery(u -> u.index(collection()).query(query).script(script));
            Long updated = response.updated();
            return updated == null ? 0 : updated;
        } catch (Exception e) {
            log.warn("updateByQuery failed:{}", query, e);
            return STATE_FAILED;
        }
    }

    public ObjectNode all(String... ids) {
        return all(Arrays.asList(ids));
    }

    public ObjectNode all(List<String> ids) {
        try {
            MgetResponse<ObjectNode> response = client.mget(m -> m.index(collection()).ids(ids), ObjectNode.class);
            ObjectNode result = DPUtil.objectNode();
            for (MultiGetResponseItem<ObjectNode> item : response.docs()) {
                if (item.isResult()) {
                    GetResult<ObjectNode> r = item.result();
                    result.replace(r.id(), r.source() == null ? DPUtil.objectNode() : r.source());
                } else {
                    result.replace(item.failure().id(), DPUtil.objectNode());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("get all sources failed:{}", DPUtil.implode(",", ids), e);
            return null;
        }
    }

    public ObjectNode one(String id) {
        try {
            GetResponse<ObjectNode> response = client.get(g -> g.index(collection()).id(id), ObjectNode.class);
            return response.source();
        } catch (Exception e) {
            log.warn("get one source failed:{}", id, e);
            return null;
        }
    }

    public List<String> add(ArrayNode sources) {
        String coll = collection();
        try {
            BulkResponse response = client.bulk(b -> {
                b.index(coll);
                for (JsonNode jsonNode : sources) {
                    ObjectNode source = (ObjectNode) jsonNode;
                    String docId = id(source);
                    b.operations(op -> op.index(idx -> {
                        if (null != docId) idx.id(docId);
                        idx.document(source);
                        return idx;
                    }));
                }
                return b;
            });
            List<String> result = new ArrayList<>();
            for (BulkResponseItem item : response.items()) {
                result.add(item.error() != null ? null : item.id());
            }
            return result;
        } catch (Exception e) {
            log.warn("add sources failed:{}", sources, e);
            return null;
        }
    }

    public String add(ObjectNode source) {
        try {
            IndexResponse response = client.index(i -> {
                i.index(collection());
                String id = id(source);
                if (null != id) i.id(id);
                i.document(source);
                return i;
            });
            return response.id();
        } catch (Exception e) {
            log.warn("add source failed:{}", source, e);
            return null;
        }
    }

    /**
     * POST /_aliases
     * {@code https://www.elastic.co/guide/en/elasticsearch/reference/7.9/indices-aliases.html}
     */
    public String alias(int fromVersion, int toVersion) {
        UpdateAliasesRequest.Builder builder = new UpdateAliasesRequest.Builder();
        builder.actions(Arrays.asList(
                Action.of(ac -> ac.remove(r -> r
                        .index(collection + "_v" + fromVersion)
                        .alias(collection))),
                Action.of(ac -> ac.add(ad -> ad
                        .index(collection + "_v" + toVersion)
                        .alias(collection)))
        ));
        return toJsonString(builder.build());
    }

    /**
     * PUT /index_name
     * {@code https://www.elastic.co/guide/en/elasticsearch/reference/7.9/indices-put-mapping.html}
     */
    public String create(boolean withAlias) {
        CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder();
        builder.index(collection);
        builder.settings(setting().build());
        builder.mappings(m -> m.withJson(new StringReader(mapping().toString())));
        if (withAlias) {
            builder.aliases(collection, a -> a.isWriteIndex(true));
        }
        return toJsonString(builder.build());
    }

    public IndexSettings.Builder setting() {
        IndexSettings.Builder builder = new IndexSettings.Builder();
        builder.numberOfShards(String.valueOf(shards));
        builder.numberOfReplicas(String.valueOf(replicas));
        builder.store(s -> {
            s.type(StorageType.Mmapfs);
            return s;
        });
        return builder;
    }

    /**
     * PUT /_template/template_name
     * {@code https://www.elastic.co/guide/en/elasticsearch/reference/7.9/index-templates.html}
     */
    public String template(boolean withAlias) {
        PutIndexTemplateRequest.Builder builder = new PutIndexTemplateRequest.Builder();
        builder.name(collection);
        builder.indexPatterns(List.of(collection + "_pattern"));
        builder.template(tm -> {
            tm.settings(setting().build());
            tm.mappings(m -> m.withJson(new StringReader(mapping().toString())));
            if (withAlias) {
                tm.aliases(collection, a -> a.isWriteIndex(true));
            }
            return tm;
        });
        return toJsonString(builder.build());
    }

    public String toJsonString(RequestBase request) {
        return JsonpUtils.toJsonString(request, client._jsonpMapper());
    }

    protected abstract ObjectNode mapping();

}
