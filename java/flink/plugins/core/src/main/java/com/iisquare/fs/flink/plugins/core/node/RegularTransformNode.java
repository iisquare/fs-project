package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.util.FlinkUtil;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.*;

public class RegularTransformNode extends Node {
    String field, pattern;
    String[] fieldNamesOld, fieldNamesNew;
    Node node;

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        field = config.get("field").asText();
        pattern = config.get("pattern").asText();
        String mode = config.get("mode").asText();
        node = source.iterator().next();
        fieldNamesOld = this.getTypeInfo().getFieldNames();
        this.typeInfo = FlinkUtil.mergeTypeInfo(mode, node.getTypeInfo(), this.typeInfo);
        fieldNamesNew = this.getTypeInfo().getFieldNames();
        return config;
    }

    private MapFunction map() {
        String field = this.field, pattern = this.pattern;
        String[] fieldNamesOld = this.fieldNamesOld, fieldNamesNew = this.fieldNamesNew;
        return new MapFunction<Map<String,Object>, Map<String,Object>>() {
            @Override
            public Map<String,Object> map(Map<String, Object> value) throws Exception {
                List<String> list = DPUtil.getMatcher(pattern, DPUtil.parseString(value.get(field)), true);
                for (int i = 0; i < fieldNamesOld.length; i++) {
                    value.put(fieldNamesOld[i], list.get(i + 1));
                }
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < fieldNamesNew.length; i++) {
                    row.put(fieldNamesNew[i], value.get(fieldNamesNew[i]));
                }
                return row;
            }
        };
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                return node.getStream().result().map(map()).name(current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                return node.getBatch().result().map(map()).name(current.getClass().getSimpleName());
            }
        };
    }
}
