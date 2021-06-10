package com.iisquare.fs.base.elasticsearch.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class ElasticsearchBase {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String FIELD_ID = "_id";
    public static final int STATE_FAILED = -1;

    @Autowired
    private RestHighLevelClient client;

    protected String collection = "fs_not_exist";
    protected int shards = 3;
    protected int replicas = 2;

    public SearchHits search(SearchSourceBuilder builder) {
        SearchRequest request = new SearchRequest(collection);
        request.source(builder);
        try {
            SearchResponse response = client.search(request, options());
            return response.getHits();
        } catch (IOException e) {
            logger.warn("search failed:" + toXContent(builder), e);
            return null;
        }
    }

    public boolean update(ObjectNode source, boolean shouldUpsertDoc) {
        UpdateRequest request = new UpdateRequest(collection, id(source));
        request.docAsUpsert(shouldUpsertDoc);
        String json = DPUtil.stringify(source);
        request.doc(json, XContentType.JSON);
        try {
            UpdateResponse response = client.update(request, options());
            DocWriteResponse.Result result = response.getResult();
            return DocWriteResponse.Result.CREATED.equals(result)
                    || DocWriteResponse.Result.UPDATED.equals(result)
                    || DocWriteResponse.Result.NOOP.equals(result);
        } catch (IOException e) {
            logger.warn("update source failed:" + json, e);
            return false;
        }
    }

    public abstract String id(ObjectNode source);

    public long delete(String id) {
        DeleteRequest request = new DeleteRequest(collection, id);
        try {
            DeleteResponse response = client.delete(request, options());
            DocWriteResponse.Result result = response.getResult();
            return DocWriteResponse.Result.DELETED.equals(result) ? 1 : 0;
        } catch (IOException e) {
            logger.warn("delete source failed:" + id, e);
            return STATE_FAILED;
        }
    }

    public long delete(String... ids) {
        if (ids.length < 1) return 0;
        DeleteByQueryRequest request = new DeleteByQueryRequest(collection);
        request.setQuery(QueryBuilders.termsQuery(FIELD_ID, ids));
        try {
            BulkByScrollResponse response = client.deleteByQuery(request, options());
            return response.getDeleted();
        } catch (IOException e) {
            logger.warn("delete sources failed:" + DPUtil.implode(",", ids), e);
            return STATE_FAILED;
        }
    }

    public ObjectNode all(String... ids) {
        MultiGetRequest request = new MultiGetRequest();
        for (String id : ids) {
            request.add(collection, id);
        }
        try {
            MultiGetResponse responses = client.mget(request, options());
            ObjectNode result = DPUtil.objectNode();
            for (MultiGetItemResponse response : responses) {
                JsonNode data = null;
                if (!response.isFailed()) {
                    data = DPUtil.parseJSON(response.getResponse().getSourceAsString());
                }
                result.replace(response.getId(), data);
            }
            return result;
        } catch (IOException e) {
            logger.warn("get all sources failed:" + DPUtil.implode(",", ids), e);
            return null;
        }
    }

    public JsonNode one(String id) {
        GetRequest request = new GetRequest(collection, id);
        try {
            GetResponse response = client.get(request, options());
            String string = response.getSourceAsString();
            return DPUtil.parseJSON(string);
        } catch (IOException e) {
            logger.warn("get one source failed:" + id, e);
            return null;
        }
    }

    public List<String> add(ArrayNode sources) {
        BulkRequest request = new BulkRequest();
        Iterator<JsonNode> iterator = sources.iterator();
        while (iterator.hasNext()) {
            ObjectNode source = (ObjectNode) iterator.next();
            String id = id(source);
            IndexRequest item = new IndexRequest(collection);
            if (null != id) item.id(id);
            String json = DPUtil.stringify(source);
            item.source(json, XContentType.JSON);
            request.add(item);
        }
        try {
            BulkResponse responses = client.bulk(request, options());
            List<String> result = new ArrayList<>();
            for (BulkItemResponse response : responses) {
                result.add(response.isFailed() ? null : response.getId());
            }
            return result;
        } catch (IOException e) {
            logger.warn("add sources failed:" + DPUtil.stringify(sources), e);
            return null;
        }
    }

    public String add(ObjectNode source) {
        IndexRequest request = new IndexRequest(collection);
        String id = id(source);
        if (null != id) request.id(id);
        String json = DPUtil.stringify(source);
        request.source(json, XContentType.JSON);
        try {
            IndexResponse response = client.index(request, options());
            return response.getId();
        } catch (IOException e) {
            logger.warn("add source failed:" + json, e);
            return null;
        }
    }

    protected RequestOptions options() {
        return RequestOptions.DEFAULT;
    }

    public String alias(int fromVersion, int toVersion) {
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        request.addAliasAction(
                IndicesAliasesRequest.AliasActions.remove().index(collection(fromVersion)).alias(collection)
        );
        request.addAliasAction(
                IndicesAliasesRequest.AliasActions.add().index(collection(toVersion)).alias(collection)
        );
        return toXContent(request);
    }

    protected String collection(int version) {
        return collection + "_v" + version;
    }

    public String indicesCreate(int version, boolean withAlias) {
        CreateIndexRequest request = new CreateIndexRequest(collection(version));
        request.settings(Settings.builder()
                .put("index.number_of_shards", shards)
                .put("index.number_of_replicas", replicas)
                .put("index.store.type", "mmapfs")
                .putList("index.store.preload", "*")
        );
        if (withAlias) request.alias(new Alias(collection));
        request.mapping(DPUtil.convertJSON(mapping(), Map.class));
        return toXContent(request);
    }

    private String toXContent(ToXContentObject request) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            ToXContent.Params params = ToXContent.EMPTY_PARAMS;
            request.toXContent(builder, params);
            return Strings.toString(builder);
        } catch (IOException e) {
            return ExceptionUtils.getStackTrace(e);
        }
    }

    protected abstract ObjectNode mapping();

}
