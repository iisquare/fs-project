package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.util.DAGUtil;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarOffsetConfigNode extends DAGConfig {
    ObjectNode config;
    @Override
    public boolean configure(JsonNode... configs) {
        config = DAGUtil.mergeConfig(configs);
        return true;
    }

    @Override
    public Object process() {
        String arg = options.at("/arg").asText("");
        String reference = options.at("/reference").asText("");
        long offset = options.at("/offset").asLong(0);
        int value = options.at("/value").asInt(0);
        String method = options.at("/method").asText("");
        int field = options.at("/field").asInt(0);
        TimeZone timezone = DAGCore.timezone(options.at("/timezone").asText());
        Locale locale = DAGCore.locale(options.at("/locale").asText());
        long datetime = DPUtil.value(config, reference).asLong();
        Calendar calendar = Calendar.getInstance(timezone, locale);
        calendar.setTime(new Date(datetime));
        switch (method) {
            case "set":
                calendar.set(field, value);
                break;
            case "add":
                calendar.add(field, value);
                break;
        }
        offset += calendar.getTimeInMillis();
        if (!DPUtil.value(config, DPUtil.empty(arg) ? reference : arg, DPUtil.toJSON(offset))) {
            throw new RuntimeException(String.format("替换配置项失败！arg:%s,reference:%s,value:%d,json:%s", arg, reference, offset, DPUtil.stringify(config)));
        }
        return config;
    }
}
