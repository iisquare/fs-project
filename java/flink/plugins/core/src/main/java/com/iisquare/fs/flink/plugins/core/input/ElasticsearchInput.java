package com.iisquare.fs.flink.plugins.core.input;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ElasticsearchInput extends RichInputFormat<Map<String, Object>, InputSplit> {

    private String clusterName;
    private String clusterNodes;
    private String collectionIndex;
    private String collectionType;
    private String query;
    private long scrollTime;
    private transient TransportClient client;
    private transient SearchResponse searchResponse;
    private transient Iterator<Map<String, Object>> iterator;

    public ElasticsearchInput(String clusterName, String clusterNodes, String collectionIndex, String collectionType, String query) {
        this.clusterName = clusterName;
        this.clusterNodes = clusterNodes;
        this.collectionIndex = collectionIndex;
        this.collectionType = collectionType;
        this.query = query;
        this.scrollTime = 60000L;
    }

    @Override
    public void configure(Configuration parameters) {
    }

    @Override
    public BaseStatistics getStatistics(BaseStatistics cachedStatistics) throws IOException {
        return null;
    }

    @Override
    public InputSplit[] createInputSplits(int minNumSplits) throws IOException {
        List<InputSplit> splits = new ArrayList<>();
        splits.add(new InputSplit() {
            @Override
            public int getSplitNumber() {
                return 0;
            }
        });
        int size = splits.size();
        return splits.toArray(new InputSplit[size]);
    }

    @Override
    public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
        return new InputSplitAssigner() {
            private int index = 0;
            @Override
            public InputSplit getNextInputSplit(String host, int taskId) {
                return (index < inputSplits.length) ? inputSplits[index++] : null;
            }
        };
    }

    @Override
    public void open(InputSplit split) throws IOException {
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put("client.transport.sniff", true)
                .build();
        client = new PreBuiltTransportClient(settings);
        for (String host : DPUtil.explode(clusterNodes, ",", " ", true)) {
            String address[] = DPUtil.explode(host, ":", " ", true);
            InetAddress inetAddress = InetAddress.getByName(address[0]);
            int port = address.length > 1 ? DPUtil.parseInt(address[1]) : 9300;
            client.addTransportAddress(new InetSocketTransportAddress(inetAddress, port));
        }
        searchResponse = client
                .prepareSearch(collectionIndex)
                .setTypes(collectionType)
                .setQuery(query.isEmpty() ? QueryBuilders.matchAllQuery() : QueryBuilders.wrapperQuery(query))
                .setScroll(new TimeValue(scrollTime))
                .setSize(100)
                .get();
        if (searchResponse == null) {
            return;
        }
        List<Map<String, Object>> hits = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            hits.add(searchHit.getSource());
        }
        iterator = hits.iterator();
    }

    @Override
    public boolean reachedEnd() throws IOException {
        if (client == null || searchResponse == null || searchResponse.getHits().getHits().length == 0 || iterator == null) {
            return true;
        }
        if (!iterator.hasNext()) {
            searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(scrollTime)).get();
            if (searchResponse == null || searchResponse.getHits().getHits().length == 0) {
                return true;
            }
            List<Map<String, Object>> hits = new ArrayList<>();
            for (SearchHit searchHit : searchResponse.getHits().getHits()) {
                hits.add(searchHit.getSource());
            }
            iterator = hits.iterator();
        }
        return false;
    }

    @Override
    public Map<String, Object> nextRecord(Map<String, Object> reuse) throws IOException {
        return iterator.next();
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}
