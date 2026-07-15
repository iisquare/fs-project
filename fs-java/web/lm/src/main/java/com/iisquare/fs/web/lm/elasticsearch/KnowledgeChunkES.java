package com.iisquare.fs.web.lm.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.mvc.ElasticsearchBase;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import com.iisquare.fs.web.lm.entity.KnowledgeChunk;
import com.iisquare.fs.web.lm.entity.KnowledgeDocument;
import com.iisquare.fs.web.lm.entity.KnowledgeSegment;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class KnowledgeChunkES extends ElasticsearchBase {

    public KnowledgeChunkES() {
        this.collection = "fs_lm_knowledge_chunk";
    }

    @Override
    public String id(ObjectNode source) {
        return source.get("id").asText();
    }

    @Override
    protected ObjectNode mapping() {
        ObjectNode source = DPUtil.objectNode();
        source.putObject("_source").put("enabled", true);
        ObjectNode properties = source.putObject("properties");
        properties.putObject("id").put("type", "integer")
                .put("index", true).put("store", true);
        properties.putObject("knowledge_id").put("type", "integer")
                .put("index", true).put("store", true);
        properties.putObject("document_id").put("type", "integer")
                .put("index", true).put("store", true);
        properties.putObject("segment_id").put("type", "integer")
                .put("index", true).put("store", true);
        properties.putObject("content").put("type", "text")
                .put("analyzer", "ik_no_word")
                .put("search_analyzer", "ik_no_word")
                .put("index", true).put("store", true);
        properties.putObject("embedding").put("type", "dense_vector")
                .put("similarity", "cosine")
                .put("dims", 1024).put("index", true);
        properties.putObject("status").put("type", "integer")
                .put("index", true).put("store", true);
        properties.putObject("created_time").put("type", "long")
                .put("index", true).put("store", true);
        properties.putObject("updated_time").put("type", "long")
                .put("index", true).put("store", true);
        properties.putObject("document_title").put("type", "text")
                .put("analyzer", "ik_no_word")
                .put("search_analyzer", "ik_no_word")
                .put("index", true).put("store", true)
                .putObject("fields").putObject("keyword")
                .put("type", "keyword").put("ignore_above", 256);
        properties.putObject("document_metadata").put("type", "keyword")
                .put("index", true).put("store", true);
        properties.putObject("document_status").put("type", "integer")
                .put("index", true).put("store", true);
        properties.putObject("segment_status").put("type", "integer")
                .put("index", true).put("store", true);
        return source;
    }

    public ObjectNode format(KnowledgeChunk chunk, KnowledgeDocument document) {
        ObjectNode source = DPUtil.objectNode();
        source.put("id", chunk.getId());
        source.put("knowledge_id", chunk.getKnowledgeId());
        source.put("document_id", chunk.getDocumentId());
        source.put("segment_id", chunk.getSegmentId());
        source.put("content", DPUtil.parseString(chunk.getContent()));
        source.replace("embedding", DPUtil.parseJSON(chunk.getEmbedding()));
        source.put("status", chunk.getStatus());
        source.put("created_time", chunk.getCreatedTime());
        source.put("updated_time", chunk.getUpdatedTime());
        if (null != document) {
            source.put("document_title", DPUtil.parseString(document.getName()));
            source.replace("document_metadata", metadata(document.getMetadata()));
        }
        return source;
    }

    public long updateSegment(KnowledgeSegment segment) {
        ObjectNode source = DPUtil.objectNode();
        source.put("segment_status", segment.getStatus());
        Query query = QueryUtil.term("segment_id", segment.getId());
        return updateByQuery(source, query);
    }

    public long updateDocument(KnowledgeDocument document) {
        ObjectNode source = DPUtil.objectNode();
        source.put("document_title", DPUtil.parseString(document.getName()));
        source.replace("document_metadata", metadata(document.getMetadata()));
        source.put("document_status", document.getStatus());
        Query query = QueryUtil.term("document_id", document.getId());
        return updateByQuery(source, query);
    }

    private ArrayNode metadata(String metadata) {
        ArrayNode result = DPUtil.arrayNode();
        JsonNode json = DPUtil.parseJSON(metadata, k -> DPUtil.objectNode());
        for (Map.Entry<String, JsonNode> entry : json.properties()) {
            result.add(entry.getKey() + "=" + entry.getValue().asText());
        }
        return result;
    }

}
