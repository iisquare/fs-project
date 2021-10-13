package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.util.DAGUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormatConfigNode extends DAGConfig {
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
        String pattern = options.at("/pattern").asText();
        TimeZone timezone = DAGCore.timezone(options.at("/timezone").asText());
        Locale locale = DAGCore.locale(options.at("/locale").asText());
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, locale);
        dateFormat.setTimeZone(timezone);
        String formatted = dateFormat.format(new Date(DPUtil.value(config, reference).asLong()));
        if (!DPUtil.value(config, DPUtil.empty(arg) ? reference : arg, DPUtil.toJSON(formatted))) {
            throw new RuntimeException(String.format("替换配置项失败！arg:%s,reference:%s,value:%s,json:%s", arg, reference, formatted, DPUtil.stringify(config)));
        }
        return config;
    }
}
