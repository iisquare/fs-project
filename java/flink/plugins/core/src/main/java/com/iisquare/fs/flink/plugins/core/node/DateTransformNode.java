package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.util.DateUtil;
import com.iisquare.fs.flink.util.FlinkUtil;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateTransformNode extends Node {

    SimpleDateFormat dateFormat;
    Node node;
    String field, dateField;

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        field = config.get("field").asText();
        String pattern = config.get("pattern").asText();
        TimeZone timeZone = TimeZone.getTimeZone(config.get("timezone").asText());
        Locale locale = DateUtil.locale(config.get("locale").asText());
        String mode = config.get("mode").asText();
        dateFormat = new SimpleDateFormat(pattern, locale);
        dateFormat.setTimeZone(timeZone);
        node = source.iterator().next();
        dateField = typeInfo.getTotalFields() > 0 ? typeInfo.getFieldNames()[0] : field;
        this.typeInfo = FlinkUtil.mergeTypeInfo(mode, node.getTypeInfo(), new RowTypeInfo(
            new TypeInformation[]{Types.SQL_TIMESTAMP}, new String[]{dateField}
        ));
        return config;

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

    private MapFunction map() {
        String[] fieldNames = this.getTypeInfo().getFieldNames();
        SimpleDateFormat dateFormat = this.dateFormat;
        String field = this.field, dateField = this.dateField;
        return new MapFunction<Map<String,Object>, Map<String,Object>>() {
            @Override
            public Map<String,Object> map(Map<String, Object> value) throws Exception {
                Date date = dateFormat.parse(DPUtil.parseString(value.get(field)));
                value.put(dateField, new Timestamp(date.getTime()));
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 0; i < fieldNames.length; i++) {
                    row.put(fieldNames[i], value.get(fieldNames[i]));
                }
                return row;
            }
        };
    }
}
