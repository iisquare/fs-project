package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.util.DateUtil;
import lombok.Getter;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.text.SimpleDateFormat;
import java.util.*;

@Getter
public class DateFormatNode extends Node {

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        String field = config.get("field").asText();
        typeInfo = new RowTypeInfo(new TypeInformation[]{Types.STRING}, new String[]{field});
        String pattern = config.get("pattern").asText();
        TimeZone timeZone = TimeZone.getTimeZone(config.get("timezone").asText());
        Locale locale = DateUtil.locale(config.get("locale").asText());
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, locale);
        dateFormat.setTimeZone(timeZone);
        Node node = source.iterator().next();
        if(null == node) return null;
        JsonNode dateGenerate = DateGenerateNode.fromNode(node);
        if(null == dateGenerate) return null;
        properties.put("formatted", dateFormat.format(new Date(dateGenerate.get("datetime").asLong())));
        return config;
    }

    public static JsonNode fromNode(Node node) {
        if(null == node) return null;
        if(DateFormatNode.class.getName().equals(node.getClass().getName())) return node.getProperties();
        return null;
    }

    public static JsonNode fromNode(Collection<Node> nodes) {
        if(null == nodes) return null;
        JsonNode result = null;
        for (Node node : nodes) {
            result = fromNode(node);
            if(null != result) return result;
        }
        return result;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                Map<String, Object> result = DPUtil.buildMap(String.class, Object.class, typeInfo.getFieldNames()[0], properties.get("formatted").asText());
                return environment().fromElements(result).name(current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                Map<String, Object> result = DPUtil.buildMap(String.class, Object.class, typeInfo.getFieldNames()[0], properties.get("formatted").asText());
                return environment().fromElements(result).name(current.getClass().getSimpleName());
            }
        };
    }

}
