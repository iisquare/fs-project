package com.iisquare.fs.flink.plugins.core.output;

import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchOutput<T> extends RichOutputFormat<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchOutput.class);
    public static final String CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS = "bulk.flush.max.actions";
    public static final String CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB = "bulk.flush.max.size.mb";
    public static final String CONFIG_KEY_BULK_FLUSH_INTERVAL_MS = "bulk.flush.interval.ms";
    private final Map<String, String> userConfig;
    private final List<InetSocketAddress> transportAddresses;
    private final ElasticsearchSinkFunction<T> elasticsearchSinkFunction;
    private transient Client client;
    private transient BulkProcessor bulkProcessor;
    private transient RequestIndexer requestIndexer;

    public ElasticsearchOutput(Map<String, String> userConfig, List<InetSocketAddress> transportAddresses, ElasticsearchSinkFunction<T> elasticsearchSinkFunction) {
        this.userConfig = userConfig;
        this.transportAddresses = transportAddresses;
        this.elasticsearchSinkFunction = elasticsearchSinkFunction;
    }

    public void configure(Configuration parameters) {

    }

    public void open(int taskNumber, int numTasks) throws IOException {
        Map<String, String> bulkConfig = new LinkedHashMap<>(userConfig);
        userConfig.remove(CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS);
        userConfig.remove(CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB);
        userConfig.remove(CONFIG_KEY_BULK_FLUSH_INTERVAL_MS);
        TransportClient transportClient = new PreBuiltTransportClient(Settings.builder().put(userConfig).build());
        for (InetSocketAddress address : transportAddresses) {
            transportClient.addTransportAddress(new InetSocketTransportAddress(address));
        }
        client = transportClient;
        BulkProcessor.Builder bulkProcessorBuilder = BulkProcessor.builder(client, new BulkProcessor.Listener() {
            public void beforeBulk(long executionId, BulkRequest request) {

            }

            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    for (BulkItemResponse itemResp : response.getItems()) {
                        if (itemResp.isFailed()) {
                            LOGGER.error(itemResp.getFailureMessage());
                        }
                    }
                }
            }

            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                LOGGER.error(failure.getMessage());
            }
        });
        bulkProcessorBuilder.setConcurrentRequests(0);
        if (bulkConfig.containsKey(CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS)) {
            bulkProcessorBuilder.setBulkActions(Integer.valueOf(bulkConfig.get(CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS)));
        }
        if (bulkConfig.containsKey(CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB)) {
            bulkProcessorBuilder.setBulkSize(new ByteSizeValue(Long.valueOf(bulkConfig.get(CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB)), ByteSizeUnit.MB));
        }
        if (bulkConfig.containsKey(CONFIG_KEY_BULK_FLUSH_INTERVAL_MS)) {
            bulkProcessorBuilder.setFlushInterval(TimeValue.timeValueMillis(Long.valueOf(bulkConfig.get(CONFIG_KEY_BULK_FLUSH_INTERVAL_MS))));
        }
        bulkProcessor = bulkProcessorBuilder.build();
        requestIndexer = new BulkProcessorIndexer(bulkProcessor);
    }

    public void writeRecord(T record) throws IOException {
        elasticsearchSinkFunction.process(record, getRuntimeContext(), requestIndexer);
    }

    public void close() throws IOException {
        if (bulkProcessor != null) {
            bulkProcessor.close();
        }
        if (client != null) {
            client.close();
        }
    }
}

class BulkProcessorIndexer implements RequestIndexer {

    private final BulkProcessor bulkProcessor;

    BulkProcessorIndexer(BulkProcessor bulkProcessor) {
        this.bulkProcessor = bulkProcessor;
    }

    @Override
    public void add(DeleteRequest... deleteRequests) {

    }

    @Override
    public void add(IndexRequest... indexRequests) {
        for (IndexRequest indexRequest : indexRequests) {
            bulkProcessor.add(indexRequest);
        }
    }

    @Override
    public void add(UpdateRequest... updateRequests) {

    }
}
