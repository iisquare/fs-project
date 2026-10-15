package com.iisquare.fs.app.flink.stream;

import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperationVariant;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.ElasticUtil;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractElasticsearchSink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch8AsyncSink;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch8AsyncSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.http.HttpHost;

/**
 * Elasticsearch 写入节点。
 *
 * Flink 2.x 采用新版连接器 flink-connector-elasticsearch8，写入语义通过 BulkOperation 表达：
 * index-覆盖写入（等价upsert）、create-仅新增、update-局部更新。
 *
 * 配置项：servers、collection、idField、tableField、mode、username、password、batchSize、flushInterval
 */
public class ElasticsearchSinkNode extends AbstractElasticsearchSink<Void> {

    String collection, idField, tableField, mode;
    Elasticsearch8AsyncSink<JsonNode> sink;

    @Override
    public DAGType<Void> outputType() {
        return FlinkTypes.VOID;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        collection = options.at("/collection").asText();
        idField = options.at("/idField").asText();
        tableField = options.at("/tableField").asText();
        mode = options.at("/mode").asText("index");
        Elasticsearch8AsyncSinkBuilder<JsonNode> builder = Elasticsearch8AsyncSinkBuilder.<JsonNode>builder()
                .setHosts(ElasticUtil.hosts(options.at("/servers").asText()).toArray(new HttpHost[0]))
                .setElementConverter(this::convert);
        String username = options.at("/username").asText();
        if (!DPUtil.empty(username)) {
            builder.setUsername(username).setPassword(options.at("/password").asText());
        }
        int batchSize = options.at("/batchSize").asInt();
        if (batchSize > 0) builder.setMaxBatchSize(batchSize);
        int flushInterval = options.at("/flushInterval").asInt();
        if (flushInterval > 0) builder.setMaxTimeInBufferMS(flushInterval);
        sink = builder.build();
        return true;
    }

    @Override
    public Void process() {
        FlinkUtil.<JsonNode>union(inputs()).sinkTo(sink)
                .setParallelism(parallelism).name(getClass().getSimpleName());
        return null;
    }

    /**
     * 将输入元素转换为Elasticsearch批量写入操作
     */
    public BulkOperationVariant convert(JsonNode element, SinkWriter.Context context) {
        String index = collection;
        if (!DPUtil.empty(tableField) && element.has(tableField)) {
            index = element.get(tableField).asText().replace("{table}", collection);
            ((ObjectNode) element).remove(tableField);
        }
        if (DPUtil.empty(index)) index = "default";
        String id = null;
        if (!DPUtil.empty(idField) && element.has(idField)) {
            id = element.get(idField).asText();
            ((ObjectNode) element).remove(idField);
        }
        JsonData document = JsonData.fromJson(DPUtil.stringify(element));
        final String target = index;
        final String docId = id;
        switch (DPUtil.empty(mode) ? "index" : mode) {
            case "create":
                return CreateOperation.of(o -> o.index(target).id(docId).document(document));
            case "update":
            case "upsert":
                return UpdateOperation.of(o -> o.index(target).id(docId)
                        .action(a -> a.doc(document).docAsUpsert(true)));
            default:
                return IndexOperation.of(o -> o.index(target).id(docId).document(document));
        }
    }
}
