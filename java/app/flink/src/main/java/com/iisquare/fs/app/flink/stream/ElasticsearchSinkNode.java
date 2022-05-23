package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.util.ElasticUtil;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.sink.AbstractElasticsearchSink;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.client.Requests;

import java.util.LinkedHashMap;
import java.util.Map;

public class ElasticsearchSinkNode extends AbstractElasticsearchSink implements ElasticsearchSinkFunction<JsonNode> {

    public static final Map<String, DocWriteRequest.OpType> modes = new LinkedHashMap(){{
        put("index", DocWriteRequest.OpType.INDEX);
        put("create", DocWriteRequest.OpType.CREATE);
        put("update", DocWriteRequest.OpType.UPDATE);
        put("upsert", DocWriteRequest.OpType.UPDATE);
    }};

    String collection, idField, tableField;
    DocWriteRequest.OpType mode;
    ElasticsearchSink<JsonNode> sink;

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        collection = options.at("/collection").asText();
        idField = options.at("/idField").asText();
        tableField = options.at("/tableField").asText();
        mode = modes.getOrDefault(options.at("/mode").asText(), DocWriteRequest.OpType.INDEX);
        ElasticsearchSink.Builder<JsonNode> builder = new ElasticsearchSink.Builder<>(
                ElasticUtil.hosts(options.at("/servers").asText()), this);
        builder.setRestClientFactory(ElasticUtil.client(
                30, 10, options.at("/username").asText(), options.at("/password").asText()));
        int batchSize = options.at("/batchSize").asInt();
        builder.setBulkFlushMaxActions(batchSize > 0 ? batchSize : -1);
        builder.setBulkFlushInterval(Math.max(-1, options.at("/flushInterval").asInt()));
        sink= builder.build();
        return true;
    }

    @Override
    public Object process() {
        FlinkUtil.union(DataStream.class, sources).addSink(sink);
        return null;
    }

    @Override
    public void process(JsonNode element, RuntimeContext ctx, RequestIndexer indexer) {
        String collection = this.collection;
        if(element.has(tableField)) {
            collection = element.get(tableField).asText().replaceAll("\\{table\\}", collection);
            ((ObjectNode) element).remove(tableField);
        }
        String id = null;
        if(element.has(idField)) {
            id = element.get(idField).asText();
            ((ObjectNode) element).remove(idField);
        }
        indexer.add(Requests.indexRequest().opType(mode).index(collection).id(id).source(DPUtil.toJSON(element, Map.class)));
    }
}
