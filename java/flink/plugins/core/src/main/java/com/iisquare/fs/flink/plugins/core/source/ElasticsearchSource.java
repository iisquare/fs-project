package com.iisquare.fs.flink.plugins.core.source;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.util.Map;

public class ElasticsearchSource extends RichSourceFunction<Map<String, Object>> {
    private TransportClient client;
    private String clusterName;
    private String clusterNodes;
    private String collectionIndex;
    private String collectionType;

    public ElasticsearchSource(String clusterName, String clusterNodes, String collectionIndex, String collectionType) {
        this.clusterName = clusterName;
        this.clusterNodes = clusterNodes;
        this.collectionIndex = collectionIndex;
        this.collectionType = collectionType;
    }

    @Override
    public void run(SourceContext<Map<String, Object>> ctx) throws Exception {
        SearchResponse scrollResp = client.prepareSearch(collectionIndex).setTypes(collectionType)
            .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
            .setScroll(new TimeValue(60000))
            .setQuery(QueryBuilders.matchAllQuery())
            .setSize(100).get(); // max of 100 hits will be returned for each scroll
        do { // Scroll until no hits are returned
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                ctx.collect(DPUtil.convertJSON(DPUtil.parseJSON(hit.getSourceAsString()), Map.class));
            }

            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
        } while(scrollResp.getHits().getHits().length != 0);
    }

    @Override
    public void cancel() {

    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        Settings settings = Settings.builder()
            .put("cluster.name", clusterName)
            .put("client.transport.sniff", true)
            .build();
        PreBuiltTransportClient preBuiltTransportClient = new PreBuiltTransportClient(settings);
        if (!"".equals(clusterNodes)) {
            for (String nodes : clusterNodes.split(",")) {
                String InetSocket[] = nodes.split(":");
                String Address = InetSocket[0];
                Integer port = Integer.valueOf(InetSocket[1]);
                preBuiltTransportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(Address), port));
            }
        }
        client = preBuiltTransportClient;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if(null != client) client.close();
    }
}
