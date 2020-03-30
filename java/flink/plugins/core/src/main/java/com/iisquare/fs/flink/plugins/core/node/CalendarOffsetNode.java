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

import java.sql.Timestamp;
import java.util.*;

@Getter
public class CalendarOffsetNode extends Node {

    public static Map<Integer, String> fieldMap = new LinkedHashMap<>();
    static {
        fieldMap.put(Calendar.YEAR, "Calendar.YEAR");
        fieldMap.put(Calendar.MONTH, "Calendar.MONTH");
        fieldMap.put(Calendar.WEEK_OF_YEAR, "Calendar.WEEK_OF_YEAR");
        fieldMap.put(Calendar.WEEK_OF_MONTH, "Calendar.WEEK_OF_MONTH");
        fieldMap.put(Calendar.DAY_OF_MONTH, "Calendar.DAY_OF_MONTH");
        fieldMap.put(Calendar.DAY_OF_YEAR, "Calendar.DAY_OF_YEAR");
        fieldMap.put(Calendar.DAY_OF_WEEK, "Calendar.DAY_OF_WEEK");
        fieldMap.put(Calendar.DAY_OF_WEEK_IN_MONTH, "Calendar.DAY_OF_WEEK_IN_MONTH");
        fieldMap.put(Calendar.HOUR, "Calendar.HOUR");
        fieldMap.put(Calendar.HOUR_OF_DAY, "Calendar.HOUR_OF_DAY");
        fieldMap.put(Calendar.MINUTE, "Calendar.MINUTE");
        fieldMap.put(Calendar.SECOND, "Calendar.SECOND");
        fieldMap.put(Calendar.MILLISECOND, "Calendar.MILLISECOND");
    }

    public static Calendar offset(JsonNode config, Long datetime) {
        TimeZone timeZone = TimeZone.getTimeZone(config.get("timezone").asText());
        Locale locale = DateUtil.locale(config.get("locale").asText());
        String doMethod = config.get("doMethod").asText();
        int doField = DPUtil.parseInt(config.get("doField").asText());
        int doValue = DPUtil.parseInt(config.get("doValue").asText());
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTime(new Date(datetime));
        if(fieldMap.containsKey(doField)) {
            switch (doMethod) {
                case "set":
                    calendar.set(doField, doValue);
                    break;
                case "add":
                    calendar.add(doField, doValue);
                    break;
            }
        }
        return calendar;
    }

    @Override
    public JsonNode run() throws Exception {
        JsonNode config = DPUtil.parseJSON(this.config);
        String field = config.get("field").asText();
        typeInfo = new RowTypeInfo(new TypeInformation[]{Types.SQL_TIMESTAMP}, new String[]{field});
        Node node = source.iterator().next();
        JsonNode dateGenerate = DateGenerateNode.fromNode(node);
        if(null == dateGenerate) return null;
        Calendar calendar = offset(config, dateGenerate.get("datetime").asLong());
        long offset = DPUtil.parseLong(config.get("offset").asText());
        properties.put("datetime", calendar.getTimeInMillis() + offset);
        return config;
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                Map<String, Object> result = DPUtil.buildMap(String.class, Object.class, typeInfo.getFieldNames()[0], new Timestamp(properties.get("datetime").asLong()));
                return environment().fromElements(result).name(current.getClass().getSimpleName());
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                Map<String, Object> result = DPUtil.buildMap(String.class, Object.class, typeInfo.getFieldNames()[0], new Timestamp(properties.get("datetime").asLong()));
                return environment().fromElements(result).name(current.getClass().getSimpleName());
            }
        };
    }

}
