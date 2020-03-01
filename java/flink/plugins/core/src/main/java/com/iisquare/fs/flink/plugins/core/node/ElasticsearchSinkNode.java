package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.plugins.core.output.ElasticsearchOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch5.ElasticsearchSink;
import org.elasticsearch.client.Requests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

@SuppressWarnings("unchecked")
public class ElasticsearchSinkNode extends Node {

    Map<String, String> userConfig;
    List<InetSocketAddress> transports;
    String collectionIndex,collectionType,idField,tableField;

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        userConfig = new HashMap<>();
        userConfig.put("bulk.flush.max.actions", config.get("maxActions").asText());
        userConfig.put("cluster.name", config.get("cluster").asText());
        transports = new ArrayList<>();
        for (String server : config.get("servers").asText().split(", *")) {
            String[] strs = DPUtil.trim(server).split(":");
            String host = strs[0];
            int port = strs.length > 1 ? DPUtil.parseInt(strs[1]) : 9300;
            transports.add(new InetSocketAddress(InetAddress.getByName(host), port));
        }
        collectionIndex = config.get("collectionIndex").asText();
        collectionType = config.get("collectionType").asText();
        idField = config.get("idField").asText();
        tableField = config.get("tableField").asText();
        return config;
    }

    private MapFunction map(Set<String> fieldDates) {
        return new MapFunction<Map<String,Object>, Map<String,Object>>() {
            @Override
            public Map<String, Object> map(Map<String, Object> row) throws Exception {
                for (String field : fieldDates) {
                    Object date = row.get(field);
                    if(null == date) continue;
                    row.put(field, new Date(((Date) date).getTime()));
                }
                return row;
            }
        };
    }

    private Set<String> fieldDates(Node node) { // 转换日期字段
        if(!Arrays.asList(node.getTypeInfo().getFieldTypes()).contains(Types.SQL_TIMESTAMP)) return null;
        String[] fieldNames = node.getTypeInfo().getFieldNames();
        TypeInformation[] fieldTypes = node.getTypeInfo().getFieldTypes();
        Set<String> fieldDates = new LinkedHashSet<>();
        for (int i = 0; i < fieldNames.length; i++) {
            if(Types.SQL_TIMESTAMP.equals(fieldTypes[i])) {
                fieldDates.add(fieldNames[i]);
            }
        }
        return fieldDates;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                for (Node node : source) {
                    DataStream<Map<String, Object>> result = node.getStream().result();
                    if(null == result) continue;
                    Set<String> fieldDates = fieldDates(node);
                    if(null != fieldDates) { // 转换日期字段
                        result = result.map(map(fieldDates)).name("Timestamp2Date");
                    }
                    result.addSink(new ElasticsearchSink(userConfig, transports, new ElasticsearchFunctionSink(
                        collectionIndex, collectionType, idField, tableField)))
                            .setParallelism(parallelism)
                            .name(current.getClass().getSimpleName());
                }
                return null;
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                for (Node node : source) {
                    DataSet<Map<String, Object>> result = node.getBatch().result();
                    if(null == result) continue;
                    Set<String> fieldDates = fieldDates(node);
                    if(null != fieldDates) { // 转换日期字段
                        result = result.map(map(fieldDates)).name("Timestamp2Date");
                    }
                    result.output(new ElasticsearchOutput(userConfig, transports, new ElasticsearchFunctionSink(
                            collectionIndex, collectionType, idField, tableField)))
                            .setParallelism(parallelism)
                            .name(current.getClass().getSimpleName());
                }
                return null;
            }
        };
    }
}

@Getter
@Setter
@AllArgsConstructor
class ElasticsearchFunctionSink implements ElasticsearchSinkFunction<Map<String, Object>> {

    private String collectionIndex;
    private String collectionType;
    private String idField;
    private String tableField;

    @Override
    public void process(Map<String, Object> map, RuntimeContext runtimeContext, RequestIndexer requestIndexer) {
        String index = collectionIndex;
        if(map.containsKey(tableField)) {
            index = map.get(tableField).toString().replaceAll("\\{table\\}", index);
            map.remove(tableField);
        }
        String id = null;
        if(map.containsKey(idField)) {
            id = map.get(idField).toString();
            map.remove(idField);
        }
        requestIndexer.add(Requests.indexRequest().index(index).type(collectionType).id(id).source(map));
    }
}
